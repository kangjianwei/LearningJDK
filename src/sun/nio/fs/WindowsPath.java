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

import java.io.IOError;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static sun.nio.fs.WindowsConstants.DRIVE_NO_ROOT_DIR;
import static sun.nio.fs.WindowsConstants.DRIVE_UNKNOWN;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_BACKUP_SEMANTICS;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OPEN_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.FILE_READ_ATTRIBUTES;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_DELETE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_READ;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_WRITE;
import static sun.nio.fs.WindowsConstants.OPEN_EXISTING;
import static sun.nio.fs.WindowsNativeDispatcher.CreateFile;
import static sun.nio.fs.WindowsNativeDispatcher.GetDriveType;
import static sun.nio.fs.WindowsNativeDispatcher.GetFullPathName;

/**
 * Windows implementation of Path
 */
// windows路径
class WindowsPath implements Path {
    
    /**
     * The maximum path that does not require long path prefix.
     * On Windows the maximum path is 260 minus 1 (NUL) but for directories it is 260 minus 12 minus 1
     * (to allow for the creation of a 8.3 file in the directory).
     */
    private static final int MAX_PATH = 247;
    
    /** Maximum extended-length path */
    private static final int MAX_LONG_PATH = 32000;
    
    /** FIXME - eliminate this reference to reduce space */
    private final WindowsFileSystem fs;     // 当前windows路径所属的windows文件系统
    
    /** normalized path */
    private final String path;              // 当前windows路径的本地化路径，已将'/'转换为'\'
    
    /** path type */
    private final WindowsPathType type;     // windows路径的类型
    
    /** root component (may be empty) */
    private final String root;              // 当前windows路径的root组件，例如盘符。可能为空，例如相对路径
    
    /** the path to use in Win32 calls. This differs from path for relative paths and has a long path prefix for all paths longer than MAX_PATH */
    private volatile WeakReference<String> pathForWin32Calls;   // 缓存对当前windows路径的解析结果
    
    /** offsets into name components (computed lazily) */
    private volatile Integer[] offsets;     // path中各个非根组件的起始下标
    
    /** computed hash code (computed lazily, no need to be volatile) */
    private int hash;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     */
    private WindowsPath(WindowsFileSystem fs, WindowsPathType type, String root, String path) {
        this.fs = fs;
        this.type = type;
        this.root = root;
        this.path = path;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 规范化/相对化/绝对化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 规范化：返回当前路径的规范化形式(包括对路径内分隔符的转换，以及对"."和".."的解析)
    @Override
    public WindowsPath normalize() {
        // 获取当前路径中非根组件的数量
        final int count = getNameCount();
        if(count == 0 || isEmpty()) {
            return this;
        }
        
        // 记录路径中需要被忽略的非根组件
        boolean[] ignore = new boolean[count];      // true => ignore name
        int remaining = count;                      // number of names remaining
        
        // multiple passes to eliminate all occurrences of "." and "name/.."
        int prevRemaining;
        
        do {
            prevRemaining = remaining;
            
            // 前一个不是"."或".."的组件的索引
            int prevName = -1;
            
            for(int i = 0; i<count; i++) {
                if(ignore[i]) {
                    continue;
                }
                
                // 返回当前路径中第i(>=0)个非根组件
                String name = elementAsString(i);
                
                // 不是"."或".."
                if(name.length()>2) {
                    prevName = i;
                    continue;
                }
                
                // 遇到是"."或其他
                if(name.length() == 1) {
                    // 忽略"."
                    if(name.charAt(0) == '.') {
                        ignore[i] = true;
                        remaining--;
                    } else {
                        prevName = i;
                    }
                    
                    continue;
                }
                
                // 不是".."
                if(name.charAt(0) != '.' || name.charAt(1) != '.') {
                    prevName = i;
                    continue;
                }
                
                // 遇到".."
                if(prevName >= 0) {
                    // name/<ignored>/.. found so mark name and ".." to be ignored
                    ignore[prevName] = true;
                    ignore[i] = true;
                    remaining = remaining - 2;
                    prevName = -1;
                } else {
                    // Cases:
                    //    C:\<ignored>\..
                    //    \\server\\share\<ignored>\..
                    //    \<ignored>..
                    if(isAbsolute() || type == WindowsPathType.DIRECTORY_RELATIVE) {
                        boolean hasPrevious = false;
                        for(int j = 0; j<i; j++) {
                            if(!ignore[j]) {
                                hasPrevious = true;
                                break;
                            }
                        }
                        if(!hasPrevious) {
                            // all proceeding names are ignored
                            ignore[i] = true;
                            remaining--;
                        }
                    }
                }
            }
        } while(prevRemaining>remaining);
        
        // no redundant names
        if(remaining == count) {
            return this;
        }
        
        // corner case - all names removed
        if(remaining == 0) {
            return (root.length() == 0) ? emptyPath() : getRoot();
        }
        
        // re-constitute the path from the remaining names.
        StringBuilder result = new StringBuilder();
        if(root != null) {
            result.append(root);
        }
        
        for(int i = 0; i<count; i++) {
            if(!ignore[i]) {
                result.append(getName(i));
                result.append("\\");
            }
        }
        
        // drop trailing slash in result
        result.setLength(result.length() - 1);
        
        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }
    
    // 相对化：返回一个相对路径，通过该相对路径，可以从当前路径访问到other路径(要求两种路径类型相同)
    @Override
    public WindowsPath relativize(Path other) {
        WindowsPath child = toWindowsPath(other);
        if(this.equals(child)) {
            return emptyPath();
        }
        
        // can only relativize paths of the same type
        if(this.type != child.type) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        
        // can only relativize paths if root component matches
        if(!this.root.equalsIgnoreCase(child.root)) {
            throw new IllegalArgumentException("'other' has different root");
        }
        
        // this path is the empty path
        if(this.isEmpty()) {
            return child;
        }
        
        WindowsPath base = this;
        
        // 如果当前路径或child中包含"."或".."，则将路径规范化
        if(base.hasDotOrDotDot() || child.hasDotOrDotDot()) {
            base = base.normalize();
            child = child.normalize();
        }
        
        // 获取当前路径中非根组件的数量
        int baseCount = base.getNameCount();
        int childCount = child.getNameCount();
        
        // 跳过共同的部分
        int n = Math.min(baseCount, childCount);
        int i = 0;
        while(i<n) {
            if(!base.getName(i).equals(child.getName(i))) {
                break;
            }
            i++;
        }
        
        // remaining elements in child
        WindowsPath childRemaining;
        boolean isChildEmpty;
        if(i == childCount) {
            childRemaining = emptyPath();
            isChildEmpty = true;
        } else {
            childRemaining = child.subpath(i, childCount);
            isChildEmpty = childRemaining.isEmpty();
        }
        
        // matched all of base
        if(i == baseCount) {
            return childRemaining;
        }
        
        // the remainder of base cannot contain ".."
        WindowsPath baseRemaining = base.subpath(i, baseCount);
        if(baseRemaining.hasDotOrDotDot()) {
            throw new IllegalArgumentException("Unable to compute relative path from " + this + " to " + other);
        }
        
        // 如果当前路径是child的子集
        if(baseRemaining.isEmpty()) {
            // 返回child中剩余的部分
            return childRemaining;
        }
        
        // number of ".." needed
        int dotdots = baseRemaining.getNameCount();
        if(dotdots == 0) {
            return childRemaining;
        }
        
        StringBuilder result = new StringBuilder();
        for(int j = 0; j<dotdots; j++) {
            result.append("..\\");
        }
        
        // append remaining names in child
        if(!isChildEmpty) {
            for(int j = 0; j<childRemaining.getNameCount(); j++) {
                result.append(childRemaining.getName(j).toString());
                result.append("\\");
            }
        }
        
        // drop trailing slash
        result.setLength(result.length() - 1);
        
        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }
    
    // 绝对化：基于当前路径解析other路径；如果other是相对路径，则返回"当前路径+other"，如果other是绝对路径，原样返回
    @Override
    public WindowsPath resolve(Path other) {
        // 将Path转换为WindowsPath
        WindowsPath otherPath = toWindowsPath(other);
        
        if(otherPath.isEmpty()) {
            return this;
        }
        
        // 当前windows路径为绝对路径或为UNC路径
        if(otherPath.isAbsolute()) {
            return otherPath;
        }
        
        switch(otherPath.type) {
            // 相对路径，如：foo
            case RELATIVE: {
                String result;
                
                if(path.endsWith("\\") || (root.length() == path.length())) {
                    result = path + otherPath.path;
                } else {
                    result = path + "\\" + otherPath.path;
                }
                
                return new WindowsPath(getFileSystem(), type, root, result);
            }
            
            // 目录相对路径，如：\foo
            case DIRECTORY_RELATIVE: {
                String result;
                
                if(root.endsWith("\\")) {
                    result = root + otherPath.path.substring(1);
                } else {
                    result = root + otherPath.path;
                }
                
                // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                return createFromNormalizedPath(getFileSystem(), result);
            }
            
            // 磁盘相对路径，如：C:foo
            case DRIVE_RELATIVE: {
                if(!root.endsWith("\\")) {
                    return otherPath;
                }
                
                // if different roots then return other
                String thisRoot = root.substring(0, root.length() - 1);
                if(!thisRoot.equalsIgnoreCase(otherPath.root)) {
                    return otherPath;
                }
                
                // same roots
                String remaining = otherPath.path.substring(otherPath.root.length());
                String result;
                if(path.endsWith("\\")) {
                    result = path + remaining;
                } else {
                    result = path + "\\" + remaining;
                }
                
                // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                return createFromNormalizedPath(getFileSystem(), result);
            }
            
            default:
                throw new AssertionError();
        }
    }
    
    // 以绝对路径形式返回当前路径(不会消除路径中的"."或"..")
    @Override
    public WindowsPath toAbsolutePath() {
        if(isAbsolute()) {
            return this;
        }
        
        // permission check as per spec
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPropertyAccess("user.dir");
        }
        
        try {
            // 当前windows路径所属的windows文件系统
            WindowsFileSystem fileSystem = getFileSystem();
            
            // 将当前windows路径转换为绝对路径后返回
            String absolutePath = getAbsolutePath();
            
            // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
            return createFromNormalizedPath(fileSystem, absolutePath);
        } catch(WindowsException x) {
            throw new IOError(new IOException(x.getMessage()));
        }
    }
    
    // 以"真实路径"形式返回当前路径(已经消除了路径中的"."或"..")，options指示对于符号链接，是否将其链接到目标文件
    @Override
    public WindowsPath toRealPath(LinkOption... options) throws IOException {
        checkRead();
        
        // 判断对于符号链接，是否将其链接到目标文件
        boolean resolveLinks = Util.followLinks(options);
        
        // 获取当前路径的真实路径，resolveLinks指示是否解析路径各组件上所有符号链接
        String realPath = WindowsLinkSupport.getRealPath(this, resolveLinks);
        
        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
        return createFromNormalizedPath(getFileSystem(), realPath);
    }
    
    /*▲ 规范化/相对化/绝对化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 当前windows路径为绝对路径或为UNC路径
    @Override
    public boolean isAbsolute() {
        return type == WindowsPathType.ABSOLUTE || type == WindowsPathType.UNC;
    }
    
    // 判断当前路径是否以other起始
    @Override
    public boolean startsWith(Path obj) {
        if(!(Objects.requireNonNull(obj) instanceof WindowsPath)) {
            return false;
        }
        
        WindowsPath other = (WindowsPath) obj;
        
        // if this path has a root component the given path's root must match
        if(!this.root.equalsIgnoreCase(other.root)) {
            return false;
        }
        
        // empty path starts with itself
        if(other.isEmpty()) {
            return this.isEmpty();
        }
        
        // roots match so compare elements
        int thisCount = getNameCount();
        int otherCount = other.getNameCount();
        if(otherCount<=thisCount) {
            while(--otherCount >= 0) {
                String thisElement = this.elementAsString(otherCount);
                String otherElement = other.elementAsString(otherCount);
                
                // FIXME: should compare in uppercase
                if(!thisElement.equalsIgnoreCase(otherElement)) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    // 判断当前路径是否以other结尾
    @Override
    public boolean endsWith(Path obj) {
        if(!(Objects.requireNonNull(obj) instanceof WindowsPath)) {
            return false;
        }
        
        WindowsPath other = (WindowsPath) obj;
        
        // other path is longer
        if(other.path.length()>this.path.length()) {
            return false;
        }
        
        // empty path ends in itself
        if(other.isEmpty()) {
            return this.isEmpty();
        }
        
        int thisCount = this.getNameCount();
        int otherCount = other.getNameCount();
        
        // given path has more elements that this path
        if(otherCount>thisCount) {
            return false;
        }
        
        // compare roots
        if(other.root.length()>0) {
            if(otherCount<thisCount) {
                return false;
            }
            
            // FIXME: should compare in uppercase
            if(!this.root.equalsIgnoreCase(other.root)) {
                return false;
            }
        }
        
        // match last 'otherCount' elements
        int off = thisCount - otherCount;
        while(--otherCount >= 0) {
            String thisElement = this.elementAsString(off + otherCount);
            String otherElement = other.elementAsString(otherCount);
            
            // FIXME: should compare in uppercase
            if(!thisElement.equalsIgnoreCase(otherElement)) {
                return false;
            }
        }
        
        return true;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前windows路径所属的windows文件系统
    @Override
    public WindowsFileSystem getFileSystem() {
        return fs;
    }
    
    // 返回当前路径的名称(路径上最后一个组件)
    @Override
    public Path getFileName() {
        int len = path.length();
        
        // represents empty path
        if(len == 0) {
            return this;
        }
        
        // represents root component only
        if(root.length() == len) {
            return null;
        }
        
        int off = path.lastIndexOf('\\');
        if(off<root.length()) {
            off = root.length();
        } else {
            off++;
        }
        
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", path.substring(off));
    }
    
    // 返回当前路径的父路径
    @Override
    public WindowsPath getParent() {
        // represents root component only
        if(root.length() == path.length()) {
            return null;
        }
        
        int off = path.lastIndexOf('\\');
        if(off<root.length()) {
            return getRoot();
        } else {
            return new WindowsPath(getFileSystem(), type, root, path.substring(0, off));
        }
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 路径组件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回根组件
    @Override
    public WindowsPath getRoot() {
        if(root.length() == 0) {
            return null;
        }
        
        return new WindowsPath(getFileSystem(), type, root, root);
    }
    
    // 返回路径组件中[beginIndex, endIndex)范围内的非根组件
    @Override
    public WindowsPath subpath(int beginIndex, int endIndex) {
        initOffsets();
        
        if(beginIndex<0) {
            throw new IllegalArgumentException();
        }
        
        if(beginIndex >= offsets.length) {
            throw new IllegalArgumentException();
        }
        
        if(endIndex>offsets.length) {
            throw new IllegalArgumentException();
        }
        
        if(beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        
        StringBuilder sb = new StringBuilder();
        Integer[] nelems = new Integer[endIndex - beginIndex];
        
        for(int i = beginIndex; i<endIndex; i++) {
            nelems[i - beginIndex] = sb.length();
            sb.append(elementAsString(i));
            if(i != (endIndex - 1)) {
                sb.append("\\");
            }
        }
        
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", sb.toString());
    }
    
    // 返回当前路径中非根组件的数量
    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }
    
    // 返回当前路径中第index(>=0)个非根组件
    @Override
    public WindowsPath getName(int index) {
        initOffsets();
        
        if(index<0 || index >= offsets.length) {
            throw new IllegalArgumentException();
        }
        
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", elementAsString(index));
    }
    
    /*▲ 路径组件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 以URI形式返回当前路径
    @Override
    public URI toUri() {
        return WindowsUriSupport.toUri(this);
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监视 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 为当前windows路径指示的目录(树)注册监听服务watcher。
     *
     * watcher  : 目录监视服务，其获取途径为：FileSystems -> FileSystem -> WatchService
     * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * modifiers: 对被监视事件的修饰，参见ExtendedWatchEventModifier(通常用这个，可以决定是否监视子目录)和SensitivityWatchEventModifier
     */
    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        // 监视服务不能为null
        if(watcher == null) {
            throw new NullPointerException();
        }
        
        // 监视服务类型需要匹配
        if(!(watcher instanceof WindowsWatchService)) {
            throw new ProviderMismatchException();
        }
        
        /*
         * When a security manager is set then we need to make a defensive copy of the modifiers and check for the Windows specific FILE_TREE modifier.
         * When the modifier is present then check that permission has been granted recursively.
         */
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            boolean watchSubtree = false;
            final int ml = modifiers.length;
            if(ml>0) {
                modifiers = Arrays.copyOf(modifiers, ml);
                int i = 0;
                while(i<ml) {
                    if(ExtendedOptions.FILE_TREE.matches(modifiers[i++])) {
                        watchSubtree = true;
                        break;
                    }
                }
            }
            
            String s = getPathForPermissionCheck();
            sm.checkRead(s);
            if(watchSubtree) {
                sm.checkRead(s + "\\-");
            }
        }
        
        /*
         * 将当前windows路径指示的目录(树)注册给监视服务watcher。
         *
         * path     : 待监视目录
         * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
         * modifiers: 对被监视事件的修饰，这里使用ExtendedWatchEventModifier，指示是否监视子目录
         */
        return ((WindowsWatchService) watcher).register(this, events, modifiers);
    }
    
    /*▲ 监视 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Creates a Path from a given path that is known to be normalized.
     */
    // 路径工厂，创建windows平台的路径对象(不会做本地化操作)，允许缓存指定的"basic"文件属性
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs, String path, BasicFileAttributes attrs) {
        try {
            // 对指定的路径path进行解析，不会对解析后的路径本地化
            WindowsPathParser.Result result = WindowsPathParser.parseNormalizedPath(path);
            
            if(attrs == null) {
                // 返回解析后的windows路径
                return new WindowsPath(fs, result.type(), result.root(), result.path());
            } else {
                // 返回包含"basic"文件属性缓存的windows路径（用于加快文件树的遍历）
                return new WindowsPathWithAttributes(fs, result.type(), result.root(), result.path(), attrs);
            }
        } catch(InvalidPathException x) {
            throw new AssertionError(x.getMessage());
        }
    }
    
    /**
     * Creates a WindowsPath from a given path that is known to be normalized.
     */
    // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs, String path) {
        return createFromNormalizedPath(fs, path, null);
    }
    
    /**
     * Creates a Path by parsing the given path.
     */
    // 使用预设的文件系统对指定的路径进行解析，返回的路径已经本地化
    static WindowsPath parse(WindowsFileSystem fs, String path) {
        // 对指定的路径path进行解析，需要对解析后的路径本地化
        WindowsPathParser.Result result = WindowsPathParser.parse(path);
        
        return new WindowsPath(fs, result.type(), result.root(), result.path());
    }
    
    /** Add long path prefix to path if required */
    // 如有必要，为路径path添加前缀
    static String addPrefixIfNeeded(String path) {
        if(path.length()>MAX_PATH) {
            if(path.startsWith("\\\\")) {
                path = "\\\\?\\UNC" + path.substring(1);
            } else {
                path = "\\\\?\\" + path;
            }
        }
        
        return path;
    }
    
    /**
     * use this path for Win32 calls
     * This method will prefix long paths with \\?\ or \\?\UNC as required.
     */
    // 解析当前windows路径为适用windows系统的绝对路径
    String getPathForWin32Calls() throws WindowsException {
        // short absolute paths can be used directly
        if(isAbsolute() && path.length()<=MAX_PATH) {
            return path;
        }
        
        /* return cached values if available */
        // 先尝试从缓存中获取
        WeakReference<String> ref = pathForWin32Calls;
        
        // 解析后的路径
        String resolved = (ref != null) ? ref.get() : null;
        if(resolved != null) {
            // Win32 path already available
            return resolved;
        }
        
        // 将当前windows路径转换为绝对路径后返回
        resolved = getAbsolutePath();   // resolve against default directory
        
        /*
         * Long paths need to have "." and ".." removed and be prefixed with "\\?\".
         * Note that it is okay to remove ".." even when it follows a link,
         * for example, it is okay for foo/link/../bar to be changed to foo/bar.
         * The reason is that Win32 APIs to access foo/link/../bar will access foo/bar anyway (which differs to Unix systems)
         */
        if(resolved.length()>MAX_PATH) {
            if(resolved.length()>MAX_LONG_PATH) {
                throw new WindowsException("Cannot access file with path exceeding " + MAX_LONG_PATH + " characters");
            }
            
            // 获取指定路径resolved的完整路径
            String fullPathName = GetFullPathName(resolved);
            
            // 如有必要，为路径path添加前缀
            resolved = addPrefixIfNeeded(fullPathName);
        }
        
        /*
         * cache the resolved path (except drive relative paths as the working directory
         * on removal media devices can change during the lifetime of the VM)
         */
        // 如果当前windows路径不是磁盘路径，则需要缓存它
        if(type != WindowsPathType.DRIVE_RELATIVE) {
            synchronized(path) {
                pathForWin32Calls = new WeakReference<String>(resolved);
            }
        }
        
        return resolved;
    }
    
    /** returns true if same drive letter */
    // 判断root1与root2是否为相同的磁盘
    private static boolean isSameDrive(String root1, String root2) {
        return Character.toUpperCase(root1.charAt(0)) == Character.toUpperCase(root2.charAt(0));
    }
    
    /** return this path resolved against the file system's default directory */
    // 将当前windows路径转换为绝对路径后返回
    private String getAbsolutePath() throws WindowsException {
        // 如果当前windows路径为绝对路径或为UNC路径
        if(isAbsolute()) {
            return path;
        }
        
        /* Relative path ("foo" for example) */
        // 如果当前路径是相对路径
        if(type == WindowsPathType.RELATIVE) {
            // 返回当前文件系统默认的工作目录(默认是当前源码根目录)
            String defaultDirectory = getFileSystem().defaultDirectory();
            
            // 如果当前路径为空，直接返回工作目录
            if(isEmpty()) {
                return defaultDirectory;
            }
            
            // 在默认工作目录后面加上相对路径信息
            if(defaultDirectory.endsWith("\\")) {
                return defaultDirectory + path;
            } else {
                return defaultDirectory + '\\' + path;
            }
        }
        
        // 获取从默认工作目录中提取的盘符信息
        String defaultRoot = getFileSystem().defaultRoot();
        
        /* Directory relative path ("\foo" for example) */
        // 如果当前路径是目录路径
        if(type == WindowsPathType.DIRECTORY_RELATIVE) {
            // 相对于工作目录的盘符的路径信息
            return defaultRoot + path.substring(1);
        }
        
        /* 至此，说明当前路径为磁盘路径 */
        
        // 判断root与defaultRoot是否为相同的磁盘
        boolean sameDrive = isSameDrive(root, defaultRoot);
        
        /* Drive relative path ("C:foo" for example) */
        // 如果当前路径的磁盘信息与默认工作目录的磁盘信息不一致
        if(!sameDrive) {
            // 当前路径的root路径
            String wd;  // relative to some other drive
            
            try {
                // 获取磁盘root的类型
                int dt = GetDriveType(root + "\\");
                
                // 如果磁盘类型未知，或者磁盘无效，则抛异常
                if(dt == DRIVE_UNKNOWN || dt == DRIVE_NO_ROOT_DIR) {
                    throw new WindowsException("");
                }
                
                // 获取当前路径的root路径
                wd = GetFullPathName(root + ".");
            } catch(WindowsException x) {
                throw new WindowsException("Unable to get working directory of drive '" + Character.toUpperCase(root.charAt(0)) + "'");
            }
            
            if(wd.endsWith("\\")) {
                wd += path.substring(root.length());
            } else {
                if(path.length()>root.length()) {
                    wd += "\\" + path.substring(root.length());
                }
            }
            
            // 将磁盘路径转换为绝对路径后返回
            return wd;
        }
        
        /* 如果当前路径的磁盘信息与默认工作目录的磁盘信息相同 */
        
        // 返回当前文件系统默认的工作目录(默认是当前源码根目录)
        String defaultDirectory = getFileSystem().defaultDirectory();
        
        /* relative to default directory */
        // 获取当前windows路径中磁盘路径后面的部分
        String remaining = path.substring(root.length());
        
        // 返回由defaultDirectory和remaining拼接的结果
        if(remaining.length() == 0) {
            return defaultDirectory;
        } else if(defaultDirectory.endsWith("\\")) {
            return defaultDirectory + remaining;
        } else {
            return defaultDirectory + "\\" + remaining;
        }
    }
    
    // 将Path强制转换为WindowsPath
    static WindowsPath toWindowsPath(Path path) {
        if(path == null) {
            throw new NullPointerException();
        }
        
        if(!(path instanceof WindowsPath)) {
            throw new ProviderMismatchException();
        }
        
        return (WindowsPath) path;
    }
    
    // 返回当前路径的类型
    WindowsPathType type() {
        return type;
    }
    
    // 判断当前路径是否为UNC路径
    boolean isUnc() {
        return type == WindowsPathType.UNC;
    }
    
    // 判断绝对化当前路径时是否需要先插入"\"
    boolean needsSlashWhenResolving() {
        if(path.endsWith("\\")) {
            return false;
        }
        
        return path.length()>root.length();
    }
    
    // 判断路径是否为空
    private boolean isEmpty() {
        return path.length() == 0;
    }
    
    // 返回一个空路径
    private WindowsPath emptyPath() {
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", "");
    }
    
    /** return true if this path has "." or ".." */
    // 判断当前路径中是否包含"."或".."
    private boolean hasDotOrDotDot() {
        // 获取当前路径中非根组件的数量
        int n = getNameCount();
        
        for(int i = 0; i<n; i++) {
            // 获取当前路径中第i(>=0)个非根组件
            String name = elementAsString(i);
            
            if(name.length() == 1 && name.charAt(0) == '.') {
                return true;
            }
            
            if(name.length() == 2 && name.charAt(0) == '.' && name.charAt(1) == '.') {
                return true;
            }
        }
        
        return false;
    }
    
    // 初始化当前路径中各个非根组件的起始下标
    private void initOffsets() {
        if(offsets != null) {
            return;
        }
        
        ArrayList<Integer> list = new ArrayList<>();
        if(isEmpty()) {
            // empty path considered to have one name element
            list.add(0);
        } else {
            int start = root.length();
            int off = root.length();
            
            while(off<path.length()) {
                if(path.charAt(off) != '\\') {
                    off++;
                } else {
                    list.add(start);
                    start = ++off;
                }
            }
            
            if(start != off) {
                list.add(start);
            }
        }
        
        synchronized(this) {
            if(offsets == null) {
                offsets = list.toArray(new Integer[0]);
            }
        }
    }
    
    // 返回当前路径中第i(>=0)个非根组件
    private String elementAsString(int i) {
        initOffsets();
        
        if(i == (offsets.length - 1)) {
            return path.substring(offsets[i]);
        }
        
        return path.substring(offsets[i], offsets[i + 1] - 1);
    }
    
    // 打开当前路径标识的文件/目录以便访问其属性
    long openForReadAttributeAccess(boolean followLinks) throws WindowsException {
        int flags = FILE_FLAG_BACKUP_SEMANTICS;
        
        // 如果对于符号链接，不需要将其链接到目标文件
        if(!followLinks) {
            // 指示系统不处理重解析点，而是按普通方式处理文件/目录
            flags |= FILE_FLAG_OPEN_REPARSE_POINT;
        }
        
        // 解析当前windows路径为适用windows系统的绝对路径
        String path = getPathForWin32Calls();
        
        return CreateFile(path,                   // 普通文件名或者设备文件名
            FILE_READ_ATTRIBUTES,   // 访问模式：只读
            (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),   // 共享模式：允许读/写/删除
            0L,                     // 指向安全属性的指针
            OPEN_EXISTING,          // 创建模式：文件必须已经存在。由设备提出要求
            flags                   // 文件属性
        );
    }
    
    /** use this message when throwing exceptions */
    String getPathForExceptionMessage() {
        return path;
    }
    
    /** use this path for permission checks */
    String getPathForPermissionCheck() {
        return path;
    }
    
    // 如果存在安全管理器，则需要检查读权限
    void checkRead() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkRead(getPathForPermissionCheck());
        }
    }
    
    // 如果存在安全管理器，则需要检查写权限
    void checkWrite() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkWrite(getPathForPermissionCheck());
        }
    }
    
    // 如果存在安全管理器，则需要检查删除权限
    void checkDelete() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkDelete(getPathForPermissionCheck());
        }
    }
    
    
    @Override
    public int compareTo(Path obj) {
        if(obj == null) {
            throw new NullPointerException();
        }
        
        String s1 = path;
        String s2 = ((WindowsPath) obj).path;
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for(int i = 0; i<min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if(c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if(c1 != c2) {
                    return c1 - c2;
                }
            }
        }
        
        return n1 - n2;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof WindowsPath) {
            return compareTo((Path) obj) == 0;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        // OK if two or more threads compute hash
        int h = hash;
        if(h != 0) {
            return h;
        }
        
        for(int i = 0; i<path.length(); i++) {
            h = 31 * h + Character.toUpperCase(path.charAt(i));
        }
        
        hash = h;
        
        return h;
    }
    
    @Override
    public String toString() {
        return path;
    }
    
    
    /**
     * Special implementation with attached/cached attributes (used to quicken file tree traversal)
     */
    // 包含"basic"文件属性缓存的windows路径（用于加快文件树的遍历）
    private static class WindowsPathWithAttributes extends WindowsPath implements BasicFileAttributesHolder {
        final WeakReference<BasicFileAttributes> ref;   // 对"basic"文件属性的缓存
        
        WindowsPathWithAttributes(WindowsFileSystem fs, WindowsPathType type, String root, String path, BasicFileAttributes attrs) {
            super(fs, type, root, path);
            ref = new WeakReference<BasicFileAttributes>(attrs);
        }
        
        // 返回缓存的"basic"文件属性
        @Override
        public BasicFileAttributes get() {
            return ref.get();
        }
        
        // 取消对目标对象的追踪
        @Override
        public void invalidate() {
            ref.clear();
        }
        
        // no need to override equals/hashCode.
    }
    
}
