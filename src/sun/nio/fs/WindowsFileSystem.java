/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// "file"文件系统在windows上的实现
class WindowsFileSystem extends FileSystem {
    
    private static final String GLOB_SYNTAX = "glob";
    private static final String REGEX_SYNTAX = "regex";
    
    // windows上支持的文件属性视图
    private static Set<String> set = new HashSet<>(Arrays.asList("user", "basic", "dos", "owner", "acl"));
    private static final Set<String> supportedFileAttributeViews = Collections.unmodifiableSet(set);
    
    private final WindowsFileSystemProvider provider;   // windows文件系统提供器
    
    // default directory (is absolute), and default root
    private final String defaultDirectory;  // 当前文件系统默认的工作目录(默认为用户工作目录，跟[user.dir]参数相关)
    private final String defaultRoot;       // 从默认工作目录中提取的盘符信息
    
    
    // 基于指定的工作目录创建平台平台相关的文件系统
    WindowsFileSystem(WindowsFileSystemProvider provider, String dir) {
        this.provider = provider;
        
        /* parse default directory and check it is absolute */
        // 对指定的路径dir进行解析，需要对解析后的路径本地化
        WindowsPathParser.Result result = WindowsPathParser.parse(dir);
        
        // 这里要求路径类型必须为绝对路径或为UNC路径
        if((result.type() != WindowsPathType.ABSOLUTE) && (result.type() != WindowsPathType.UNC)) {
            throw new AssertionError("Default directory is not an absolute path");
        }
        
        this.defaultDirectory = result.path();
        
        this.defaultRoot = result.root();
    }
    
    
    // 构造windows平台下的路径对象，返回的路径已经本地化
    @Override
    public final Path getPath(String first, String... more) {
        String path;
        
        if(more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for(String segment : more) {
                if(segment.length()>0) {
                    if(sb.length()>0) {
                        sb.append('\\');
                    }
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        
        // 使用当前文件系统对指定的路径进行解析，返回的路径已经本地化
        return WindowsPath.parse(this, path);
    }
    
    // 返回创建了当前文件系统的windows文件系统提供器
    @Override
    public FileSystemProvider provider() {
        return provider;
    }
    
    // 返回当前文件系统使用的file分隔符；例如windows上是'\'，linux/mac上是'/'
    @Override
    public String getSeparator() {
        return "\\";
    }
    
    // 判断当前文件系统是否已经开启
    @Override
    public boolean isOpen() {
        return true;
    }
    
    // 判断当前文件系统是否只读
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    // 关闭当前文件系统
    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    // 返回当前文件系统中的根路径列表
    @Override
    public Iterable<Path> getRootDirectories() {
        int drives = 0;
        try {
            // 获取驱动器的盘符列表
            drives = WindowsNativeDispatcher.GetLogicalDrives();
        } catch(WindowsException x) {
            // shouldn't happen
            throw new AssertionError(x.getMessage());
        }
        
        // iterate over roots, ignoring those that the security manager denies
        ArrayList<Path> result = new ArrayList<>();
        SecurityManager sm = System.getSecurityManager();
        
        // 遍历26个字母，查看当前文件系统中到底有哪些盘符
        for(int i = 0; i<=25; i++) {  // 0->A, 1->B, 2->C...
            if((drives & (1 << i)) != 0) {
                String root = (char) ('A' + i) + ":\\";
                if(sm != null) {
                    try {
                        sm.checkRead(root);
                    } catch(SecurityException x) {
                        continue;
                    }
                }
                
                // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                WindowsPath windowsPath = WindowsPath.createFromNormalizedPath(this, root);
                
                // 记录找到的盘符(根路径)
                result.add(windowsPath);
            }
        }
        
        return Collections.unmodifiableList(result);
    }
    
    // 返回当前文件系统中的文件存储列表
    @Override
    public Iterable<FileStore> getFileStores() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            try {
                sm.checkPermission(new RuntimePermission("getFileStoreAttributes"));
            } catch(SecurityException se) {
                return Collections.emptyList();
            }
        }
        
        return new Iterable<FileStore>() {
            public Iterator<FileStore> iterator() {
                return new FileStoreIterator();
            }
        };
    }
    
    /*
     * 返回当前文件系统支持的文件属性视图
     *
     * 不同平台的支持情况：
     *         windows linux mac
     * "user"     √      √
     * "basic"    √      √    √
     * "dos"      √      √
     * "owner"    √      √    √
     * "acl"      √
     * "posix"           √    √
     * "unix"            √    √
     * "jrt"      √      √    √
     * "zip"      √      √    √
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }
    
    // 返回一个由指定正则构造的路径匹配器；其中，syntaxAndInput可以是"glob"正则或"regex"正则（参见Globs类）
    @Override
    public PathMatcher getPathMatcher(String syntaxAndInput) {
        // 如果syntaxAndInput中不存在":"，则抛异常
        int pos = syntaxAndInput.indexOf(':');
        if(pos<=0 || pos == syntaxAndInput.length()) {
            throw new IllegalArgumentException();
        }
        
        // 获取正则类型："glob"类型或"regex"类型
        String syntax = syntaxAndInput.substring(0, pos);
        
        // 获取正则表达式
        String input = syntaxAndInput.substring(pos + 1);
        
        String regex;
        
        // 如果是"glob"类型的正则，则需要转换
        if(syntax.equalsIgnoreCase(GLOB_SYNTAX)) {
            // 将"glob"类型的正则表达式转换为"regex"类型的正则表达式
            regex = Globs.toWindowsRegexPattern(input);
        } else {
            // 如果是"regex"类型的正则，直接使用
            if(syntax.equalsIgnoreCase(REGEX_SYNTAX)) {
                regex = input;
            } else {
                throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
            }
        }
        
        /* match in unicode_case_insensitive */
        // 解析正则表达式regex，解析时不区分大小写且允许匹配Unicode符号
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        
        /* return matcher */
        return new PathMatcher() {
            // 判断指定的路径是否符合预设的模式，如果符合，则对其访问
            @Override
            public boolean matches(Path path) {
                // 利用当前Pattern中的正则表达式创建Matcher以进行匹配动作
                Matcher matcher = pattern.matcher(path.toString());
                
                // 判断是否匹配
                return matcher.matches();
            }
        };
    }
    
    // 返回一个目录监视服务，目前仅在"file"文件系统上提供支持
    @Override
    public WatchService newWatchService() throws IOException {
        return new WindowsWatchService(this);
    }
    
    // 返回一个账户服务，可用来搜索本机的用户和组信息
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return LookupService.instance;
    }
    
    // 返回当前文件系统默认的工作目录(默认是当前源码根目录)
    String defaultDirectory() {
        return defaultDirectory;
    }
    
    // 返回从默认工作目录中提取的盘符信息
    String defaultRoot() {
        return defaultRoot;
    }
    
    
    // 账户服务
    private static class LookupService {
        static final UserPrincipalLookupService instance = new UserPrincipalLookupService() {
            
            // 查询指定名称的user
            @Override
            public UserPrincipal lookupPrincipalByName(String name) throws IOException {
                return WindowsUserPrincipals.lookup(name);
            }
            
            // 查询指定名称的group
            @Override
            public GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException {
                UserPrincipal user = WindowsUserPrincipals.lookup(group);
                if(!(user instanceof GroupPrincipal)) {
                    throw new UserPrincipalNotFoundException(group);
                }
                
                return (GroupPrincipal) user;
            }
        };
    }
    
    /**
     * Iterator returned by getFileStores method.
     */
    // 当前文件系统中的文件存储列表
    private class FileStoreIterator implements Iterator<FileStore> {
        private final Iterator<Path> roots;     // 当前文件系统中的根路径列表
        private FileStore next;
        
        FileStoreIterator() {
            this.roots = getRootDirectories().iterator();
        }
        
        @Override
        public synchronized boolean hasNext() {
            if(next != null) {
                return true;
            }
            next = readNext();
            return next != null;
        }
        
        @Override
        public synchronized FileStore next() {
            if(next == null) {
                next = readNext();
            }
            
            if(next == null) {
                throw new NoSuchElementException();
            } else {
                FileStore result = next;
                next = null;
                return result;
            }
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private FileStore readNext() {
            assert Thread.holdsLock(this);
            
            for(; ; ) {
                if(!roots.hasNext()) {
                    return null;
                }
                
                WindowsPath root = (WindowsPath) roots.next();
                
                // ignore if security manager denies access
                try {
                    root.checkRead();
                } catch(SecurityException x) {
                    continue;
                }
                
                try {
                    FileStore fs = WindowsFileStore.create(root.toString(), true);
                    if(fs != null) {
                        return fs;
                    }
                } catch(IOException ioe) {
                    // skip it
                }
            }
        }
    }
    
}
