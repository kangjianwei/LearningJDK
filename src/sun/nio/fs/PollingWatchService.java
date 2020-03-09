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

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Simple WatchService implementation that uses periodic tasks to poll
 * registered directories for changes.  This implementation is for use on
 * operating systems that do not have native file change notification support.
 */
/*
 * WatchService的简单实现，使用定时任务轮询注册目录中的更改，识别更改的依据是文件的最后修改时间。
 * 此实现适用于不支持本地文件更改通知的操作系统平台。
 *
 * 另外，通过此类，可以大致观察WatchService的工作模式。
 *
 * 注：下文中提到的"主线程"是指构造监视服务的线程，该线程不一定是传统意义上main方法所在的线程，其本身可能也是一个子线程。
 */
class PollingWatchService extends AbstractWatchService {
    
    /** map of registrations */
    // 文件标识到监视键的映射；文件标识指示了被监视的目录
    private final Map<Object, PollingWatchKey> map = new HashMap<>();
    
    /** used to execute the periodic tasks that poll for changes */
    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutor;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 在主线程中构造监视服务
    PollingWatchService() {
        /* TBD: Make the number of threads configurable */
        // 构造定时服务执行器
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(null, r, "FileSystemWatcher", 0, false);
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Register the given file with this watch service
     */
    /*
     * 将路径path指示的目录(树)注册给当前监视服务。
     *
     * path     : 待监视目录
     * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * modifiers: 对被监视事件的修饰，这里使用SensitivityWatchEventModifier，指示监视服务运行的频繁程度
     */
    @Override
    WatchKey register(final Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        // check events - CCE will be thrown if there are invalid elements
        final Set<WatchEvent.Kind<?>> eventSet = new HashSet<>(events.length);  // 记录标准监视事件
        
        // 收集注册的监视事件类型
        for(WatchEvent.Kind<?> event : events) {
            // 标准监视事件
            if(event == StandardWatchEventKinds.ENTRY_CREATE || event == StandardWatchEventKinds.ENTRY_MODIFY || event == StandardWatchEventKinds.ENTRY_DELETE) {
                eventSet.add(event);
                continue;
            }
            
            // OVERFLOW is ignored
            if(event == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            
            // null/unsupported
            if(event == null) {
                throw new NullPointerException("An element in event set is 'null'");
            }
            
            throw new UnsupportedOperationException(event.name());
        }
        
        if(eventSet.isEmpty()) {
            throw new IllegalArgumentException("No events to register");
        }
        
        /* Extended modifiers may be used to specify the sensitivity level */
        // 监视服务灵敏度，灵敏度越高，服务搜集目录变动信息的操作越频繁
        int sensitivity = 10;
        
        if(modifiers.length>0) {
            // 遍历被监视事件的修饰符
            for(WatchEvent.Modifier modifier : modifiers) {
                if(modifier == null) {
                    throw new NullPointerException();
                }
                
                if(ExtendedOptions.SENSITIVITY_HIGH.matches(modifier)) {
                    sensitivity = ExtendedOptions.SENSITIVITY_HIGH.parameter();
                } else if(ExtendedOptions.SENSITIVITY_MEDIUM.matches(modifier)) {
                    sensitivity = ExtendedOptions.SENSITIVITY_MEDIUM.parameter();
                } else if(ExtendedOptions.SENSITIVITY_LOW.matches(modifier)) {
                    sensitivity = ExtendedOptions.SENSITIVITY_LOW.parameter();
                } else {
                    throw new UnsupportedOperationException("Modifier not supported");
                }
            }
        }
        
        /* check if watch service is closed */
        if(!isOpen()) {
            throw new ClosedWatchServiceException();
        }
        
        // registration is done in privileged block as it requires the attributes of the entries in the directory.
        try {
            int value = sensitivity;
            return AccessController.doPrivileged(new PrivilegedExceptionAction<PollingWatchKey>() {
                @Override
                public PollingWatchKey run() throws IOException {
                    // 执行注册行为
                    return doPrivilegedRegister(path, eventSet, value);
                }
            });
        } catch(PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if(cause != null && cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new AssertionError(pae);
        }
    }
    
    /** registers directory returning a new key if not already registered or existing key if already registered */
    /*
     * 将路径path指示的目录(树)注册给当前监视服务。
     *
     * path                 : 待监视目录
     * events               : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * sensitivityInSeconds : 监视服务运行的频率，即每隔多少秒运行一次
     */
    private PollingWatchKey doPrivilegedRegister(Path path, Set<? extends WatchEvent.Kind<?>> events, int sensitivityInSeconds) throws IOException {
        /* check file is a directory and get its file key if possible */
        // 获取指定路径标识的文件的基础文件属性
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        // 确保被监视的是目录
        if(!attrs.isDirectory()) {
            throw new NotDirectoryException(path.toString());
        }
        
        // 返回唯一标识待监视目录的对象。如果文件标识不可用(例如windows上)，则返回null
        Object fileKey = attrs.fileKey();
        // 此处要求必须支持该标识
        if(fileKey == null) {
            throw new AssertionError("File keys must be supported");
        }
        
        // grab close lock to ensure that watch service cannot be closed
        synchronized(closeLock()) {
            if(!isOpen()) {
                throw new ClosedWatchServiceException();
            }
            
            PollingWatchKey watchKey;
            synchronized(map) {
                // 从缓存中获取监视键
                watchKey = map.get(fileKey);
                
                // 新的注册
                if(watchKey == null) {
                    watchKey = new PollingWatchKey(path, this, fileKey);
                    map.put(fileKey, watchKey);
                } else {
                    // 关闭轮询器，不再监视目录
                    watchKey.disable();
                }
            }
            
            // 启动固定周期任务，每隔sensitivityInSeconds秒就检测一次文件变动信息
            watchKey.enable(events, sensitivityInSeconds);
            
            return watchKey;
        }
        
    }
    
    /*▲ 注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程
     *
     * 具体的关闭行为是：关闭定时任务执行器
     */
    @Override
    void implClose() throws IOException {
        synchronized(map) {
            for(Map.Entry<Object, PollingWatchKey> entry : map.entrySet()) {
                PollingWatchKey watchKey = entry.getValue();
                
                watchKey.disable();     // 关闭轮询器，不再监视目录
                watchKey.invalidate();  // 标记监视键watchKey为无效
            }
            
            map.clear();
        }
        
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                // 关闭定时任务执行器
                scheduledExecutor.shutdown();
                return null;
            }
        });
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Entry in directory cache to record file last-modified-time and tick-count
     */
    // 缓存被监视文件的最后修改时间以及存在性标识
    private static class CacheEntry {
        private long lastModified;  // 被监视文件的最后修改时间
        private int lastTickCount;  // 存在性标识，每次服务之后都会更新
        
        CacheEntry(long lastModified, int lastTickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = lastTickCount;
        }
        
        int lastTickCount() {
            return lastTickCount;
        }
        
        long lastModified() {
            return lastModified;
        }
        
        void update(long lastModified, int tickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = tickCount;
        }
    }
    
    /**
     * WatchKey implementation that encapsulates a map of the entries of the entries in the directory.
     * Polling the key causes it to re-scan the directory and queue keys when entries are added, modified, or deleted.
     */
    /*
     * 监视键的回退实现，适用于不支持本地文件更改通知的操作系统平台
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
    private class PollingWatchKey extends AbstractWatchKey {
        
        // 唯一标识被监视目录的对象。如果文件标识不可用(例如windows上)，则返回null；参见BasicFileAttributes#fileKey()
        private final Object fileKey;
        
        /** indicates if the key is valid */
        // 指示当前监视键是否有效
        private volatile boolean valid;
        
        /** current event set */
        // 注册的监视事件集，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
        private Set<? extends WatchEvent.Kind<?>> events;
        
        /** the result of the periodic task that causes this key to be polled */
        // 轮询器，执行固定周期任务
        private ScheduledFuture<?> poller;
        
        /** used to detect files that have been deleted */
        // 存在性标识，指示被监视文件在第tickCount次监视中依然存在
        private int tickCount;
        
        /** map of entries in directory */
        // 记录当前被监视的文件
        private Map<Path, CacheEntry> entries;
        
        PollingWatchKey(Path dir, PollingWatchService watcher, Object fileKey) throws IOException {
            super(dir, watcher);
            this.fileKey = fileKey;
            this.valid = true;
            this.tickCount = 0;
            this.entries = new HashMap<Path, CacheEntry>();
            
            // 获取指定实体的目录流，用来搜寻目录内的子文件/目录（不会过滤任何子项）
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                // 遍历监视的目录树(对于符号链接，不链接目标文件)，记录每个(子)文件的最后修改时间
                for(Path entry : stream) {
                    long lastModified = Files.getLastModifiedTime(entry, LinkOption.NOFOLLOW_LINKS).toMillis();
                    
                    // 返回entry的名称(路径上最后一个组件)
                    Path path = entry.getFileName();
                    
                    // 将被监视的文件记录到缓存中
                    entries.put(path, new CacheEntry(lastModified, tickCount));
                }
            } catch(DirectoryIteratorException e) {
                throw e.getCause();
            }
        }
        
        // 启动固定周期任务，每隔period秒就检测一次文件变动信息
        void enable(Set<? extends WatchEvent.Kind<?>> events, long period) {
            synchronized(this) {
                // update the events
                this.events = events;
                
                // create the periodic task
                Runnable thunk = new Runnable() {
                    public void run() {
                        poll(); // 待执行任务：轮询目录以检测新文件/已修改文件/已删除文件
                    }
                };
                
                // 执行固定周期任务：每隔period秒执行一次
                this.poller = scheduledExecutor.scheduleAtFixedRate(thunk, period, period, TimeUnit.SECONDS);
            }
        }
        
        /**
         * Polls the directory to detect for new files, modified files, or deleted files.
         */
        // 轮询器的主要工作：轮询目录以检测新文件/已修改文件/已删除文件
        synchronized void poll() {
            if(!valid) {
                return;
            }
    
            // (全局)存在性标识递增
            tickCount++;
    
            // open directory
            DirectoryStream<Path> stream = null;
            try {
                // 获取被监视目录
                Path path = watchable();
        
                // 获取path的目录流，用来搜寻目录内的子文件/目录（不会过滤任何子项）
                stream = Files.newDirectoryStream(path);
            } catch(IOException x) {
                // directory is no longer accessible so cancel key
                cancel();   // 由主线程调用：向子线程(工作线程)请求"取消"服务，并阻塞主线程；直到"取消"完成后，唤醒主线程
                signal();
                return;
            }
    
            // iterate over all entries in directory
            try {
                // 遍历被监视目录的子文件
                for(Path entry : stream) {
                    long lastModified = 0L;
                    try {
                        // 获取entry的最后修改时间(对于符号链接，不链接目标文件)
                        lastModified = Files.getLastModifiedTime(entry, LinkOption.NOFOLLOW_LINKS).toMillis();
                    } catch(IOException x) {
                        /*
                         * unable to get attributes of entry.
                         * If file has just been deleted then we'll report it as deleted on the next poll.
                         */
                        continue;
                    }
        
                    // 在缓存中查找指定的文件信息
                    CacheEntry cache = entries.get(entry.getFileName());
        
                    // 如果缓存中没有找到该文件，说明这是一个新文件
                    if(cache == null) {
                        // 将新文件加入缓存
                        entries.put(entry.getFileName(), new CacheEntry(lastModified, tickCount));
            
                        // 如果用户有意监视ENTRY_CREATE事件，则包装ENTRY_CREATE事件到监视键，并唤醒等待获取监视键的主线程
                        if(events.contains(StandardWatchEventKinds.ENTRY_CREATE)) {
                            signalEvent(StandardWatchEventKinds.ENTRY_CREATE, entry.getFileName());
                
                            // 如果用户不需要监视ENTRY_CREATE事件，则将ENTRY_CREATE事件回退为ENTRY_MODIFY事件，并将其封装到到监视键，并唤醒等待获取监视键的主线程
                        } else if(events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                            /*
                             * if ENTRY_CREATE is not enabled and ENTRY_MODIFY is enabled then queue event to avoid missing out
                             * on modifications to the file immediately after it is created.
                             */
                            signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, entry.getFileName());
                        }
            
                        continue;
                    }
        
                    // 至此，说明遇到了旧文件；对于旧文件，检查该文件是否发生改动，依据是判断其最后修改时间是否发生了变化
                    if(cache.lastModified != lastModified) {
                        // 如果用户有意监视ENTRY_MODIFY事件，则包装ENTRY_MODIFY事件到监视键，并唤醒等待获取监视键的主线程
                        if(events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                            signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, entry.getFileName());
                        }
                    }
        
                    // entry in cache so update poll time */
                    // 对于旧文件，更新其最后修改时间与存在性标识
                    cache.update(lastModified, tickCount);
                }
            } catch(DirectoryIteratorException e) {
                // ignore for now; if the directory is no longer accessible then the key will be cancelled on the next poll
            } finally {
    
                // close directory stream
                try {
                    stream.close();
                } catch(IOException x) {
                    // ignore
                }
            }
    
            /* iterate over cache to detect entries that have been deleted */
            /*
             * 遍历缓存中所有文件信息，如果其存在性标识发生了改变，说明这个文件已被删除了。
             * 因为如果没被删除的话，前面会更新其存在性标识到最新值。
             */
            Iterator<Map.Entry<Path, CacheEntry>> iterator = entries.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<Path, CacheEntry> mapEntry = iterator.next();
                CacheEntry entry = mapEntry.getValue();
                if(entry.lastTickCount() != tickCount) {
                    Path name = mapEntry.getKey();
            
                    // remove from map and queue delete event (if enabled)
                    iterator.remove();
            
                    // 如果用户有意监视ENTRY_DELETE事件，则包装ENTRY_DELETE事件到监视键，并唤醒等待获取监视键的主线程
                    if(events.contains(StandardWatchEventKinds.ENTRY_DELETE)) {
                        signalEvent(StandardWatchEventKinds.ENTRY_DELETE, name);
                    }
                }
            }
        }
        
        /*
         * 由主线程调用：向子线程(工作线程)请求"取消"服务，并阻塞主线程；直到"取消"完成后，唤醒主线程。
         *
         * 注：有时候子线程(工作线程)处理事件受挫时，也会执行该方法，以取消服务，参见PollingWatchService#poll()
         */
        @Override
        public void cancel() {
            valid = false;
            synchronized(map) {
                // 返回唯一标识被监视目录的对象
                Object fileKey = fileKey();
                map.remove(fileKey);
            }
            
            // 关闭轮询器，不再监视目录
            disable();
        }
        
        // 判断当前监视键是否有效；如果监视键被取消，或监视服务被关闭，则该监视键无效
        @Override
        public boolean isValid() {
            return valid;
        }
        
        // 标记当前监视键为无效
        void invalidate() {
            valid = false;
        }
        
        // 返回唯一标识被监视目录的对象。如果文件标识不可用(例如windows上)，则返回null；参见BasicFileAttributes#fileKey()
        Object fileKey() {
            return fileKey;
        }
        
        // 关闭轮询器，不再监视目录
        void disable() {
            synchronized(this) {
                if(poller != null) {
                    // 中止轮询器，这里应用了"取消"操作
                    poller.cancel(false);
                }
            }
        }
    }
    
}
