/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsConstants.ERROR_MORE_DATA;
import static sun.nio.fs.WindowsConstants.ERROR_NOTIFY_ENUM_DIR;
import static sun.nio.fs.WindowsConstants.FILE_ACTION_ADDED;
import static sun.nio.fs.WindowsConstants.FILE_ACTION_MODIFIED;
import static sun.nio.fs.WindowsConstants.FILE_ACTION_REMOVED;
import static sun.nio.fs.WindowsConstants.FILE_ACTION_RENAMED_NEW_NAME;
import static sun.nio.fs.WindowsConstants.FILE_ACTION_RENAMED_OLD_NAME;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_BACKUP_SEMANTICS;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OVERLAPPED;
import static sun.nio.fs.WindowsConstants.FILE_LIST_DIRECTORY;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_ATTRIBUTES;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_CREATION;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_DIR_NAME;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_FILE_NAME;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_LAST_WRITE;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_SECURITY;
import static sun.nio.fs.WindowsConstants.FILE_NOTIFY_CHANGE_SIZE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_DELETE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_READ;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_WRITE;
import static sun.nio.fs.WindowsConstants.INVALID_HANDLE_VALUE;
import static sun.nio.fs.WindowsConstants.OPEN_EXISTING;
import static sun.nio.fs.WindowsNativeDispatcher.CancelIo;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.CompletionStatus;
import static sun.nio.fs.WindowsNativeDispatcher.CreateEvent;
import static sun.nio.fs.WindowsNativeDispatcher.CreateFile;
import static sun.nio.fs.WindowsNativeDispatcher.CreateIoCompletionPort;
import static sun.nio.fs.WindowsNativeDispatcher.GetOverlappedResult;
import static sun.nio.fs.WindowsNativeDispatcher.GetQueuedCompletionStatus;
import static sun.nio.fs.WindowsNativeDispatcher.PostQueuedCompletionStatus;
import static sun.nio.fs.WindowsNativeDispatcher.ReadDirectoryChangesW;

/**
 * Win32 implementation of WatchService based on ReadDirectoryChangesW.
 */
/*
 * windows上实现的对目录的监视服务
 *
 * 注：下文中提到的"主线程"是指构造监视服务的线程，该线程不一定是传统意义上main方法所在的线程，其本身可能也是一个子线程。
 */
class WindowsWatchService extends AbstractWatchService {
    
    // 主线程需要注册/取消/关闭服务时，会将此标志传递给子线程(工作线程)，以便唤醒子线程(工作线程)处理来自主线程的请求
    private static final int WAKEUP_COMPLETION_KEY = 0;
    
    // 所有需要关注的变动事件，这是注册给底层系统看的
    private static final int ALL_FILE_NOTIFY_EVENTS = FILE_NOTIFY_CHANGE_FILE_NAME | FILE_NOTIFY_CHANGE_DIR_NAME | FILE_NOTIFY_CHANGE_ATTRIBUTES | FILE_NOTIFY_CHANGE_SIZE | FILE_NOTIFY_CHANGE_LAST_WRITE | FILE_NOTIFY_CHANGE_CREATION | FILE_NOTIFY_CHANGE_SECURITY;
    
    // background thread to service I/O completion port
    private final Poller poller;    // 轮询器，用来在后台完成监视服务
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an I/O completion port and a daemon thread to service it
     */
    /*
     * 在主线程构造监视服务，具体行为是：
     * 1.创建一个IO完成端口
     * 2.创建(守护)工作线程，在其中启动一个轮询器
     */
    WindowsWatchService(WindowsFileSystem fs) throws IOException {
        // create I/O completion port
        long port = 0L;
        
        try {
            // 创建一个完成端口对象，返回其端口引用
            port = CreateIoCompletionPort(INVALID_HANDLE_VALUE, 0, 0);
        } catch(WindowsException x) {
            throw new IOException(x.getMessage());
        }
        
        // 创建一个轮询器
        this.poller = new Poller(fs, this, port);
        
        // 在新建的守护工作线程中启动轮询器
        this.poller.start();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 由主线程调用：向子线程(工作线程)请求"注册"服务(将path注册给当前监视服务轮询器)，并阻塞主线程；
     * 直到"注册"完成后，唤醒主线程，返回监视键
     *
     * path     : 待监视目录(树)
     * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * modifiers: 对被监视事件的修饰，这里使用ExtendedWatchEventModifier，指示是否监视子目录
     */
    @Override
    WatchKey register(Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        // 将注册行为委托给轮询器
        return poller.register(path, events, modifiers);
    }
    
    /*▲ 注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程
     *
     * 具体的关闭行为是：解除对当前监视目录的监视，释放监视键缓存，标记相关的监视键为无效，并结束子线程(工作线程)
     */
    @Override
    void implClose() throws IOException {
        // delegate to poller
        poller.close();
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /** file key to unique identify (open) directory */
    // 文件(目录)键，用来标识一个文件(目录)对象
    private static class FileKey {
        private final int volSerialNumber;  // 卷序列号
        private final int fileIndexHigh;    // 文件标识(高位)
        private final int fileIndexLow;     // 文件标识(低位)
        
        FileKey(int volSerialNumber, int fileIndexHigh, int fileIndexLow) {
            this.volSerialNumber = volSerialNumber;
            this.fileIndexHigh = fileIndexHigh;
            this.fileIndexLow = fileIndexLow;
        }
        
        @Override
        public int hashCode() {
            return volSerialNumber ^ fileIndexHigh ^ fileIndexLow;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            
            if(!(obj instanceof FileKey)) {
                return false;
            }
            
            FileKey other = (FileKey) obj;
            
            if(this.volSerialNumber != other.volSerialNumber) {
                return false;
            }
            
            if(this.fileIndexHigh != other.fileIndexHigh) {
                return false;
            }
            
            return this.fileIndexLow == other.fileIndexLow;
        }
    }
    
    /**
     * Windows implementation of WatchKey.
     */
    /*
     * 监视键在windows上的实现
     *
     * 监视键是主线程与子线程(工作线程)的沟通桥梁，其生成方式是：
     * 主线程向子线程(工作线程)发出"注册"请求，子线程(工作线程)处理完该请求后，返回继续的监视键。
     *
     * 监视键记录了被监视目录，以及包含了从子线程(工作线程)反馈的目标事件。
     *
     * 子线程(工作线程)发现(命中)监视事件时，会视情形将其封装为WatchEvent；
     * 随后，将WatchEvent存入监视键的待处理事件集合，并且再次将监视键加入到监视服务中为其准备的队列容器中；
     * 最后，主线程会从上述队列容器中取出监视键，并进一步从监视键中获取监视到的事件。
     */
    private static class WindowsWatchKey extends AbstractWatchKey {
        
        // 文件(目录)键，用来标识被监视的目录
        private final FileKey fileKey;  // file key (used to detect existing registrations)
        
        // 被监视目录的句柄
        private volatile long handle = INVALID_HANDLE_VALUE;    // handle to directory
        
        // 注册的监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
        private Set<? extends WatchEvent.Kind<?>> events;   // interest events
        
        // 是否监视子目录
        private boolean watchSubtree;   // subtree
        
        /*
         * buffer用来存储ReadDirectoryChangesW的lpBytesReturned和lpOverlapped参数
         * countAddress指向的内存存储lpBytesReturned参数的值
         * overlappedAddress指向的内存存储lpOverlapped参数的值
         */
        private NativeBuffer buffer;    // buffer for change events
        private long countAddress;      // pointer to bytes returned (in buffer)
        private long overlappedAddress; // pointer to overlapped structure (in buffer)
        
        // 与当前被监视目录关联的完成键，在同一个监视服务下是唯一的
        private int completionKey;      // completion key (used to map I/O completion to WatchKey)
        
        
        private boolean errorStartingOverlapped;    // flag indicates that ReadDirectoryChangesW failed and overlapped I/O operation wasn't started
        
        WindowsWatchKey(Path dir, AbstractWatchService watcher, FileKey fileKey) {
            super(dir, watcher);
            this.fileKey = fileKey;
        }
        
        // 初始化监视键的其它参数
        WindowsWatchKey init(long handle, Set<? extends WatchEvent.Kind<?>> events, boolean watchSubtree, NativeBuffer buffer, long countAddress, long overlappedAddress, int completionKey) {
            this.handle = handle;
            this.events = events;
            this.watchSubtree = watchSubtree;
            this.buffer = buffer;
            this.countAddress = countAddress;
            this.overlappedAddress = overlappedAddress;
            this.completionKey = completionKey;
            return this;
        }
        
        /*
         * 由主线程调用：向子线程(工作线程)请求"取消"服务，并阻塞主线程；直到"取消"完成后，唤醒主线程。
         *
         * 注：有时候子线程(工作线程)处理事件受挫时，也会执行该方法，以取消服务，参见PollingWatchService#poll()
         */
        @Override
        public void cancel() {
            // 如果当前监视键有效
            if(isValid()) {
                // delegate to poller
                ((WindowsWatchService) watcher()).poller.cancel(this);
            }
        }
        
        // 判断当前监视键是否有效；如果监视键被取消，或监视服务被关闭，则该监视键无效
        @Override
        public boolean isValid() {
            return handle != INVALID_HANDLE_VALUE;
        }
        
        /** Invalidate the key, assumes that resources have been released */
        // 设置当前监视键为无效
        void invalidate() {
            // 返回当前监视键使用的监视服务(多个目录可以共用一个监视服务)
            WindowsWatchService watchService = (WindowsWatchService) watcher();
            
            // 释放当前监视键关联的资源
            watchService.poller.releaseResources(this);
            
            handle = INVALID_HANDLE_VALUE;
            buffer = null;
            countAddress = 0;
            overlappedAddress = 0;
            errorStartingOverlapped = false;
        }
        
        // 返回注册在监视键内的监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
        Set<? extends WatchEvent.Kind<?>> events() {
            return events;
        }
        
        // 为监视键注册监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
        void setEvents(Set<? extends WatchEvent.Kind<?>> events) {
            this.events = events;
        }
        
        // 返回被监视目录的句柄
        long handle() {
            return handle;
        }
        
        // 返回文件(目录)键，用来标识被监视的目录
        FileKey fileKey() {
            return fileKey;
        }
        
        // 是否监视子目录
        boolean watchSubtree() {
            return watchSubtree;
        }
        
        NativeBuffer buffer() {
            return buffer;
        }
        
        long countAddress() {
            return countAddress;
        }
        
        long overlappedAddress() {
            return overlappedAddress;
        }
        
        int completionKey() {
            return completionKey;
        }
        
        boolean isErrorStartingOverlapped() {
            return errorStartingOverlapped;
        }
        
        void setErrorStartingOverlapped(boolean value) {
            errorStartingOverlapped = value;
        }
        
    }
    
    /**
     * Background thread to service I/O completion port.
     */
    // 监视服务后台使用的轮询器
    private static class Poller extends AbstractPoller {
        
        private static final Unsafe UNSAFE = Unsafe.getUnsafe();
        
        /*
         * typedef struct _OVERLAPPED {
         *     ULONG_PTR  Internal;
         *     ULONG_PTR  InternalHigh;
         *     union {
         *         struct {
         *             DWORD Offset;
         *             DWORD OffsetHigh;
         *         };
         *         PVOID  Pointer;
         *     };
         *     HANDLE    hEvent;
         * } OVERLAPPED;
         */
        private static final short SIZEOF_DWORD = 4;
        private static final short SIZEOF_OVERLAPPED = 32; // 20 on 32-bit
        private static final short OFFSETOF_HEVENT = (UNSAFE.addressSize() == 4) ? (short) 16 : 24;
        
        
        /*
         * typedef struct _FILE_NOTIFY_INFORMATION {
         *     DWORD NextEntryOffset;
         *     DWORD Action;
         *     DWORD FileNameLength;
         *     WCHAR FileName[1];
         * } FileNameLength;
         */
        private static final short OFFSETOF_NEXTENTRYOFFSET = 0;
        private static final short OFFSETOF_ACTION = 4;
        private static final short OFFSETOF_FILENAMELENGTH = 8;
        private static final short OFFSETOF_FILENAME = 12;
        
        // size of per-directory buffer for events (FIXME - make this configurable)
        // Need to be less than 4*16384 = 65536. DWORD align.
        private static final int CHANGES_BUFFER_SIZE = 16 * 1024;
        
        private final WindowsFileSystem fs;         // 当前使用的文件系统
        
        private final WindowsWatchService watcher;  // 当前使用的监视服务
        
        private final long port;                    // 轮询器绑定的完成端口对象的引用
        
        // maps completion key to WatchKey
        private final Map<Integer, WindowsWatchKey> ck2key; // 缓存<完成键编号, 监视键>
        
        // maps file key to WatchKey
        private final Map<FileKey, WindowsWatchKey> fk2key; // 缓存<被监视的目录, 监视键>
        
        // unique completion key for each directory. native completion key capacity is 64 bits on Win64.
        private int lastCompletionKey;              // 完成键编号，与监视键绑定，不断递增，保持唯一
        
        Poller(WindowsFileSystem fs, WindowsWatchService watcher, long port) {
            this.fs = fs;
            this.watcher = watcher;
            this.port = port;
            this.ck2key = new HashMap<>();
            this.fk2key = new HashMap<>();
            this.lastCompletionKey = 0;
        }
        
        /**
         * Poller main loop
         */
        /*
         * 当前轮询器在(守护)工作线程中启动后，会进入该循环。
         *
         * 底层没有文件变动事件时，陷入阻塞。
         *
         * 当底层传来一批文件变动事件时，该主循环会被唤醒；
         * 随后，该主循环将依次将当前批次的变动事件全部存入监视键，并唤醒等待获取监视键的主线程。
         *
         * 除此之外，主线程可以通过PostQueuedCompletionStatus()向当前子线程(工作线程)发送注册/取消/关闭的消息。
         */
        @Override
        public void run() {
            for(; ; ) {
                // 来自底层完成端口的通知
                CompletionStatus info;
                
                try {
                    // 无限阻塞，直到"完成端口"port有新的通知就绪时，获取该通知的内容并返回
                    info = GetQueuedCompletionStatus(port);
                } catch(WindowsException x) {
                    // this should not happen
                    x.printStackTrace();
                    return;
                }
                
                /*
                 * 被主线程唤醒，传递主线程发来的注册/取消/关闭消息
                 *
                 * 参见AbstractPoller#wakeup()方法
                 */
                if(info.completionKey() == WAKEUP_COMPLETION_KEY) {
                    // 处理来自主线程的注册/取消/关闭请求
                    boolean shutdown = processRequests();
                    
                    if(shutdown) {
                        return;
                    }
                    
                    continue;
                }
                
                /*
                 * 被本地线程唤醒，反馈底层监视到的事件
                 */
                
                /* map completionKey to get WatchKey */
                // 从缓存中获取相关的监视键
                WindowsWatchKey watchKey = ck2key.get((int) info.completionKey());
                if(watchKey == null) {
                    /*
                     * We get here when a registration is changed.
                     * In that case the directory is closed which causes an event with the old completion key.
                     */
                    continue;
                }
                
                boolean criticalError = false;
                
                // Iocp的扩展错误信息，如超时、断网等
                int errorCode = info.error();
                
                // 写入或读取重叠结构的字节数
                int messageSize = info.bytesTransferred();
                
                if(errorCode == ERROR_NOTIFY_ENUM_DIR) {
                    /* buffer overflow */
                    // 包装OVERFLOW事件到监视键，并唤醒等待获取监视键的主线程
                    watchKey.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                } else if(errorCode != 0 && errorCode != ERROR_MORE_DATA) {
                    // ReadDirectoryChangesW failed
                    criticalError = true;
                } else {
                    /*
                     * ERROR_MORE_DATA is a warning about incomplete data transfer over TCP/UDP stack.
                     * For the case [messageSize] is zero in the most of cases.
                     */
                    // 如果重叠结构中存在有效数据
                    if(messageSize>0) {
                        /* process non-empty events */
                        // 将底层反馈来的监视事件封装到监视键watchKey中，成为待处理事件；随后，会唤醒阻塞在获取监视键操作上的主线程
                        processEvents(watchKey, messageSize);
                    } else if(errorCode == 0) {
                        /* insufficient buffer size, not described, but can happen */
                        // 包装OVERFLOW事件到监视键，并唤醒等待获取监视键的主线程
                        watchKey.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                    }
                    
                    // start read for next batch of changes
                    try {
                        // 开始监听下一批变动事件
                        ReadDirectoryChangesW(watchKey.handle(), watchKey.buffer().address(), CHANGES_BUFFER_SIZE, watchKey.watchSubtree(), ALL_FILE_NOTIFY_EVENTS, watchKey.countAddress(), watchKey.overlappedAddress());
                    } catch(WindowsException x) {
                        // no choice but to cancel key
                        criticalError = true;
                        watchKey.setErrorStartingOverlapped(true);
                    }
                }
                
                // 如果出现了严重错误
                if(criticalError) {
                    /*
                     * 由子线程调用，执行"取消"操作。
                     * 取消/作废(已注册目录关联的)监视键，即表示不再监视其指示的目录
                     */
                    implCancelKey(watchKey);
                    
                    // 将监视键watchKey(已经无效)加入到所属监视服务的阻塞队列中，并标记其状态为激活(此过程中会唤醒阻塞在出队操作上的线程)
                    watchKey.signal();
                }
            }// for(; ; )
        }
        
        // 唤醒阻塞在GetQueuedCompletionStatus()上的工作线程
        @Override
        void wakeup() throws IOException {
            try {
                PostQueuedCompletionStatus(port, WAKEUP_COMPLETION_KEY);
            } catch(WindowsException x) {
                throw new IOException(x.getMessage());
            }
        }
        
        /**
         * Register a directory for changes as follows:
         *
         * 1. Open directory
         * 2. Read its attributes (and check it really is a directory)
         * 3. Assign completion key and associated handle with completion port
         * 4. Call ReadDirectoryChangesW to start (async) read of changes
         * 5. Create or return existing key representing registration
         */
        /*
         * 由子线程调用，执行"注册"操作。
         * 将指定的目录(树)注册到监听服务中，返回被监视目录(树)关联的监视键。
         */
        @Override
        Object implRegister(Path obj, Set<? extends WatchEvent.Kind<?>> events, WatchEvent.Modifier... modifiers) {
            WindowsPath dir = (WindowsPath) obj;
            
            // 是否监视目录树而不是单个目录(是否递归监控)
            boolean watchSubtree = false;
            
            /* FILE_TREE modifier allowed */
            // 遍历监视事件修饰符
            for(WatchEvent.Modifier modifier : modifiers) {
                // 如果使用了监视事件修饰符FILE_TREE，则需要监视整个目录树
                if(ExtendedOptions.FILE_TREE.matches(modifier)) {
                    watchSubtree = true;
                } else {
                    if(modifier == null) {
                        return new NullPointerException();
                    }
                    
                    if(!ExtendedOptions.SENSITIVITY_HIGH.matches(modifier) && !ExtendedOptions.SENSITIVITY_MEDIUM.matches(modifier) && !ExtendedOptions.SENSITIVITY_LOW.matches(modifier)) {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }
            
            // 待监视目录的引用
            long handle;
            try {
                // 解析dir为适用windows系统的绝对路径
                String path = dir.getPathForWin32Calls();
                
                // 打开被监视目录(必须存在)，返回其句柄(引用)
                handle = CreateFile(path, FILE_LIST_DIRECTORY, (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE), OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED);
            } catch(WindowsException x) {
                return x.asIOException(dir);
            }
            
            boolean registered = false;
            try {
                // read attributes and check file is a directory
                WindowsFileAttributes attrs;
                try {
                    // 获取handle文件的windows文件属性信息
                    attrs = WindowsFileAttributes.readAttributes(handle);
                } catch(WindowsException x) {
                    return x.asIOException(dir);
                }
    
                // 如果监视目标不是目录，在抛出异常
                if(!attrs.isDirectory()) {
                    return new NotDirectoryException(dir.getPathForExceptionMessage());
                }
    
                /* check if this directory is already registered */
                // 标识一个文件(目录)对象
                FileKey fk = new FileKey(attrs.volSerialNumber(), attrs.fileIndexHigh(), attrs.fileIndexLow());
    
                // 尝试获取fk映射的监视键
                WindowsWatchKey existingWatchKey = fk2key.get(fk);
    
                /*
                 * if already registered and we're not changing the subtree modifier
                 * then simply update the event and return the key.
                 */
                // 如果该目录已被监视，且监视范围没变(监视范围指是否监视子目录)
                if(existingWatchKey != null && watchSubtree == existingWatchKey.watchSubtree()) {
                    // 为监视键注册监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
                    existingWatchKey.setEvents(events);
        
                    // 返回已存在的监视键
                    return existingWatchKey;
                }
    
                /* Can overflow the int type capacity. Skip WAKEUP_COMPLETION_KEY value */
                int completionKey = ++lastCompletionKey;
                if(completionKey == WAKEUP_COMPLETION_KEY) {
                    completionKey = ++lastCompletionKey;
                }
    
                /* associate handle with completion port */
                try {
                    // 将一个文件句柄handle与已有的完成端口port关联到一起
                    CreateIoCompletionPort(handle, port, completionKey);
                } catch(WindowsException x) {
                    return new IOException(x.getMessage());
                }
    
                // allocate memory for events, including space for other structures needed to do overlapped I/O
                int size = CHANGES_BUFFER_SIZE + SIZEOF_DWORD + SIZEOF_OVERLAPPED;
    
                // 返回容量至少为size的缓存(owner为null)，用来为lpOverlapped和lpBytesReturned填充数据
                NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);
                long bufferAddress = buffer.address();                              // 参见ReadDirectoryChangesW##lpBuffer
                long overlappedAddress = bufferAddress + size - SIZEOF_OVERLAPPED;  // 参见ReadDirectoryChangesW##lpOverlapped
                long countAddress = overlappedAddress - SIZEOF_DWORD;               // 参见ReadDirectoryChangesW##lpBytesReturned
    
                /* zero the overlapped structure */
                // 为申请的内存批量填充初值，这里用0填充
                UNSAFE.setMemory(overlappedAddress, SIZEOF_OVERLAPPED, (byte) 0);
    
                /* start async read of changes to directory */
                try {
                    // 创建一个未命名的事件，并将其设置为给定OVERLAPPED结构中的hEvent字段
                    createAndAttachEvent(overlappedAddress);
        
                    // 监视指定的目录，未来如果有感兴趣的事件到达，会填充到OVERLAPPED结构中的hEvent字段中
                    ReadDirectoryChangesW(handle, bufferAddress, CHANGES_BUFFER_SIZE, watchSubtree, ALL_FILE_NOTIFY_EVENTS, countAddress, overlappedAddress);
                } catch(WindowsException x) {
                    // 关闭附加到指定OVERLAPPED结构的事件
                    closeAttachedEvent(overlappedAddress);
                    buffer.release();
                    return new IOException(x.getMessage());
                }
    
                // 监视键在windows上的实现，指示被监视目录及其关联信息
                WindowsWatchKey watchKey;
    
                // 如果指定的目录还未被监视
                if(existingWatchKey == null) {
                    /* not registered so create new watch key */
                    // 初始化一个监视键
                    watchKey = new WindowsWatchKey(dir, watcher, fk);
        
                    // 初始化监视键的其它参数
                    watchKey.init(handle, events, watchSubtree, buffer, countAddress, overlappedAddress, completionKey);
        
                    /* map file key to watch key */
                    // 缓存<监视目录, 监视键>到fk2key映射中
                    fk2key.put(fk, watchKey);
        
                    // 如果指定的目录已被监视，且监视范围发生了变化(监视范围指是否监视子目录)
                } else {
                    /*
                     * directory already registered so need to:
                     * 1. remove mapping from old completion key to existing watch key
                     * 2. release existing key's resources (handle/buffer)
                     * 3. re-initialize key with new handle/buffer
                     */
                    // 移除之前的监视键
                    ck2key.remove(existingWatchKey.completionKey());
        
                    // 释放旧的监视键关联的资源
                    releaseResources(existingWatchKey);
        
                    // 生成新的监视键
                    watchKey = existingWatchKey.init(handle, events, watchSubtree, buffer, countAddress, overlappedAddress, completionKey);
                }
    
                /* map completion map to watch key */
                // <完成端口, 监视键>映射，用来缓存新的监视键
                ck2key.put(completionKey, watchKey);
    
                // 标记为已注册
                registered = true;
    
                // 返回被监视文件关联的监视键
                return watchKey;
    
            } finally {
                // 如果没注册成功
                if(!registered) {
                    // 关闭一个打开的句柄
                    CloseHandle(handle);
                }
            }
        }
        
        /** cancel single key */
        /*
         * 由子线程调用，执行"取消"操作。
         * 取消/作废(已注册目录关联的)监视键，即表示不再监视其指示的目录
         */
        @Override
        void implCancelKey(WatchKey obj) {
            WindowsWatchKey key = (WindowsWatchKey) obj;
            
            // 如果监视键有效
            if(key.isValid()) {
                // 将其从轮询器的缓存中移除
                fk2key.remove(key.fileKey());
                ck2key.remove(key.completionKey());
                
                // 设置该监视键为无效
                key.invalidate();
            }
        }
        
        /*
         * 由子线程调用，执行"关闭"操作。
         * 关闭监视服务，会取消所有(已注册目录关联的)监视键
         */
        @Override
        void implCloseAll() {
            // 将所有监视键取消/作废
            ck2key.values().forEach(WindowsWatchKey::invalidate);
            
            // 清空轮询器中的缓存
            fk2key.clear();
            ck2key.clear();
            
            // 关闭一个打开的对象句柄
            CloseHandle(port);
        }
        
        /**
         * Cancels the outstanding I/O operation on the directory
         * associated with the given key and releases the associated
         * resources.
         */
        // 释放指定的监视键关联的资源
        private void releaseResources(WindowsWatchKey key) {
            
            if(!key.isErrorStartingOverlapped()) {
                try {
                    CancelIo(key.handle());
                    GetOverlappedResult(key.handle(), key.overlappedAddress());
                } catch(WindowsException expected) {
                    // expected as I/O operation has been cancelled
                }
            }
            
            // 关闭一个打开的句柄
            CloseHandle(key.handle());
            
            // 关闭附加到指定OVERLAPPED结构的事件
            closeAttachedEvent(key.overlappedAddress());
            
            // 释放监视键使用的本地缓存所占的本地内存
            key.buffer().free();
        }
        
        /**
         * Creates an unnamed event and set it as the hEvent field in the given OVERLAPPED structure
         */
        // 创建一个未命名的事件，并将其设置为给定OVERLAPPED结构中的hEvent字段
        private void createAndAttachEvent(long overlapped) throws WindowsException {
            // 用来创建或打开一个命名的或无名的事件对象
            long hEvent = CreateEvent(false, false);
            
            // 计算偏移地址
            long address = overlapped + OFFSETOF_HEVENT;
            
            // 向本地内存地址address处存入本地指针值hEvent
            UNSAFE.putAddress(address, hEvent);
        }
        
        /**
         * Closes the event attached to the given OVERLAPPED structure.
         * A no-op if there isn't an event attached.
         */
        // 关闭附加到指定OVERLAPPED结构的事件
        private void closeAttachedEvent(long overlappedAddress) {
            long hEvent = UNSAFE.getAddress(overlappedAddress + OFFSETOF_HEVENT);
            if(hEvent != 0 && hEvent != INVALID_HANDLE_VALUE) {
                CloseHandle(hEvent);
            }
        }
        
        /** Translate file change action into watch event */
        // 将文件更改操作转换为监视事件类型
        private WatchEvent.Kind<?> translateActionToEvent(int action) {
            switch(action) {
                case FILE_ACTION_MODIFIED:
                    return StandardWatchEventKinds.ENTRY_MODIFY;
                
                case FILE_ACTION_ADDED:
                case FILE_ACTION_RENAMED_NEW_NAME:
                    return StandardWatchEventKinds.ENTRY_CREATE;
                
                case FILE_ACTION_REMOVED:
                case FILE_ACTION_RENAMED_OLD_NAME:
                    return StandardWatchEventKinds.ENTRY_DELETE;
                
                default:
                    return null;  // action not recognized
            }
        }
        
        /** process events (list of FILE_NOTIFY_INFORMATION structures) */
        // 将底层反馈来的监视事件封装到监视键watchKey中，成为待处理事件；随后，会唤醒阻塞在获取监视键操作上的主线程
        private void processEvents(WindowsWatchKey watchKey, int size) {
            long address = watchKey.buffer().address();
            
            int nextOffset;
            
            do {
                // 用来获取文件更改操作的类型
                int action = UNSAFE.getInt(address + OFFSETOF_ACTION);
                
                /* map action to event */
                // 将文件更改操作转换为监视事件类型
                WatchEvent.Kind<?> kind = translateActionToEvent(action);
                
                // 获取注册在监视键内的监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
                Set<? extends Kind<?>> events = watchKey.events();
                
                // 如果底层反馈的kind类型的事件是用户所关心的，则需要处理它
                if(events.contains(kind)) {
                    // 获取触发该事件的文件名的长度
                    int nameLengthInBytes = UNSAFE.getInt(address + OFFSETOF_FILENAMELENGTH);
                    if((nameLengthInBytes % 2) != 0) {
                        throw new AssertionError("FileNameLength is not a multiple of 2");
                    }
                    
                    char[] nameAsArray = new char[nameLengthInBytes / 2];
                    
                    // 将文件名称拷贝到nameAsArray中
                    UNSAFE.copyMemory(null, address + OFFSETOF_FILENAME, nameAsArray, Unsafe.ARRAY_CHAR_BASE_OFFSET, nameLengthInBytes);
                    
                    /* create FileName and queue event */
                    // 路径工厂，使用变动内容的相对路径作为事件上下文，创建windows平台的路径对象(不会做本地化操作)
                    WindowsPath path = WindowsPath.createFromNormalizedPath(fs, new String(nameAsArray));
                    
                    /*
                     * 将底层响应的kind类型的事件封装到监视键watchKey，如果该事件"有效"，则path指示触发该事件的子目录。
                     * 随后，将封装好的监视键标记为激活，并将其加入到所属监视服务的阻塞队列中，
                     * 最后，唤醒阻塞在出队操作上的主线程，参见WatchService中的take()和poll()。
                     */
                    watchKey.signalEvent(kind, path);
                }
                
                // 获取下一个事件
                nextOffset = UNSAFE.getInt(address + OFFSETOF_NEXTENTRYOFFSET);
                
                address += nextOffset;
            } while(nextOffset != 0);
        }
    }
    
}
