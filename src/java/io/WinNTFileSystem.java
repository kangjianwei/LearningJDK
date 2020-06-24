/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.Locale;
import java.util.Properties;
import sun.security.action.GetPropertyAction;

/**
 * Unicode-aware FileSystem for Windows NT/2000.
 *
 * @author Konstantin Kladko
 * @since 1.4
 */
// Windows文件系统
class WinNTFileSystem extends FileSystem {
    
    private final char slash;       // 标准路径内部的分隔符：Windows系统上是'\'，类Unix系统上是'/'
    private final char altSlash;    // 其他可用的路径内部的分隔符：Windows系统上是'/'，类Unix系统上是'\'
    private final char semicolon;   // 路径之间的分隔符：Windows系统上是';'，类Unix系统上是':'
    private final String userDir;   // 用户工作目录[user.dir]
    
    // 盘符对应的目录相对路径缓存
    private static String[] driveDirCache = new String[26];
    
    /**
     * Caches for canonicalization results to improve startup performance.
     * The first cache handles repeated canonicalizations of the same path name.
     * The prefix cache handles repeated canonicalizations within the same directory,
     * and must not create results differing from the true canonicalization algorithm in canonicalize_md.c.
     * For this reason the prefix cache is conservative and is not used for complex path names.
     */
    private ExpiringCache cache = new ExpiringCache();
    private ExpiringCache prefixCache = new ExpiringCache();
    
    static {
        initIDs();
    }
    
    public WinNTFileSystem() {
        Properties props = GetPropertyAction.privilegedGetProperties();
        slash = props.getProperty("file.separator").charAt(0);
        semicolon = props.getProperty("path.separator").charAt(0);
        altSlash = (this.slash == '\\') ? '/' : '\\';
        userDir = normalize(props.getProperty("user.dir"));
    }
    
    // 返回路径内部的分隔符：Windows系统上是'\'，类Unix系统上是'/'
    @Override
    public char getSeparator() {
        return slash;
    }
    
    // 返回路径之间的分隔符：Windows系统上是';'，类Unix系统上是':'
    @Override
    public char getPathSeparator() {
        return semicolon;
    }
    
    /**
     * Check that the given pathname is normal.
     * If not, invoke the real normalizer on the part of the pathname that requires normalization.
     * This way we iterate through the whole pathname string only once.
     */
    /*
     * 返回本地化路径
     *
     * 1.提取磁盘前缀和UNC前缀
     * 2.将路径分隔符统一为'\'
     * 3.忽略末尾的路径分隔符
     */
    @Override
    public String normalize(String path) {
        int n = path.length();  // path中包含的char的数量
        
        char prev = 0;  // 记下前一个字符
        
        for(int i = 0; i<n; i++) {
            char c = path.charAt(i);
            
            // 遇到了其他可用的路径内分隔符
            if(c == altSlash) {
                return normalize(path, n, (prev == slash) ? i - 1 : i);
            }
            
            // 遇到了两个连续的标准路径内分隔符
            if((c == slash) && (prev == slash) && (i>1)) {
                return normalize(path, n, i - 1);
            }
            
            if((c == ':') && (i>1)) {
                return normalize(path, n, 0);
            }
            
            prev = c;
        }
        
        if(prev == slash) {
            return normalize(path, n, n - 1);
        }
        
        return path;
    }
    
    /*
     * 返回本地化路径的前缀长度
     *
     * 0 - 相对路径，如相对路径"a\b"
     * 1 - 磁盘相对路径，如"\a\b"
     * 2 - UNC路径，如"\\a\b"，或目录相对路径，如"c:a\b"
     * 3 - 绝对路径，如"c:\a\b"
     */
    @Override
    public int prefixLength(String path) {
        int n = path.length();
        if(n == 0) {
            return 0;
        }
        
        char c0 = path.charAt(0);
        char c1 = (n>1) ? path.charAt(1) : 0;
        
        if(c0 == slash) {
            if(c1 == slash) {
                /* Absolute UNC pathname "\\\\foo" */
                return 2;
            }
            
            /* Drive-relative "\\foo" */
            return 1;
        }
        
        if(isLetter(c0) && (c1 == ':')) {
            if((n>2) && (path.charAt(2) == slash)) {
                /* Absolute local pathname "z:\\foo" */
                return 3;
            }
            
            /* Directory-relative "z:foo" */
            return 2;
        }
        
        /* Completely relative */
        return 0;
    }
    
    // 将指定文件的本地化路径解析为绝对路径后返回
    @Override
    public String resolve(File file) {
        // 获取File的本地化路径
        String path = file.getPath();
        
        // 获取本地化路径的前缀长度
        int pl = file.getPrefixLength();
        
        // UNC路径
        if((pl == 2) && (path.charAt(0) == slash)) {
            return path;                        /* UNC */
        }
        
        // 绝对路径
        if(pl == 3) {
            return path;                        /* Absolute local */
        }
        
        // 相对路径
        if(pl == 0) {
            // 用户工作目录[user.dir]+"\"+相对路径
            return getUserPath() + slashify(path); /* Completely relative */
        }
        
        // 磁盘相对路径
        if(pl == 1) {                          /* Drive-relative */
            String up = getUserPath();  // 用户工作目录[user.dir]
            
            // 返回用户工作目录中的盘符信息
            String ud = getDrive(up);
            if(ud != null) {
                return ud + path;
            }
            
            return up + path;                   /* User dir is a UNC path */
        }
        
        // 目录相对路径
        if(pl == 2) {                          /* Directory-relative */
            // 用户工作目录[user.dir]
            String up = getUserPath();
            
            // 返回用户工作目录中的盘符信息
            String ud = getDrive(up);
            // 如果目录相对路径中的盘符信息与用户工作目录中的盘符信息一致(区分大小写)
            if((ud != null) && path.startsWith(ud)) {
                // 将目录相对路径中的盘符用用户工作目录替代
                return up + slashify(path.substring(2));
            }
            
            // 获取盘符
            char drive = path.charAt(0);
            
            /*
             * 获取指定的盘符对应的相对目录信息
             *
             * 假设[user.dir]为"C:\Users\kang\Desktop\test"
             * 那么对于'c'，返回"\Users\kang\Desktop\test"
             * 而对于'd'，仅返回"\"
             */
            String dir = getDriveDirectory(drive);
            
            String np;
            if(dir != null) {
                /*
                 * When resolving a directory-relative path that refers to a drive other than the current drive,
                 * insist that the caller have read permission on the result
                 */
                // 拼接，如将c:a转换为c:\Users\kang\Desktop\test\a，将d:a转换为d:\\a
                String p = drive + ':' + dir + slashify(path.substring(2));
                SecurityManager security = System.getSecurityManager();
                try {
                    if(security != null) {
                        security.checkRead(p);
                    }
                } catch(SecurityException x) {
                    /* Don't disclose the drive's directory in the exception */
                    throw new SecurityException("Cannot resolve path " + path);
                }
                return p;
            }
            
            // 找不到盘符对应的相对目录时的拼接，如将a:a转换为a:\a
            return drive + ":" + slashify(path.substring(2)); /* fake it */
        }
        
        throw new InternalError("Unresolvable path: " + path);
    }
    
    /*
     * 返回拼接后的本地化路径
     *
     * parent后面的"\"将被忽略，child前面的"\"会被考虑。
     * 如果parent的格式形如"c:"，则拼接为目录相对路径。
     * 其他一般情形下，会在parent与child之间添加'\'
     */
    @Override
    public String resolve(String parent, String child) {
        int pn = parent.length();
        if(pn == 0) {
            return child;
        }
        
        int cn = child.length();
        if(cn == 0) {
            return parent;
        }
        
        int childStart = 0;
        int parentEnd = pn;
        
        // parent是否为"目录相对路径"，如"c:"
        boolean isDirectoryRelative = pn == 2 && isLetter(parent.charAt(0)) && parent.charAt(1) == ':';
        
        // 如果child以"\"开头
        if((cn>1) && (child.charAt(0) == slash)) {
            // 如果child以"\\"开头
            if(child.charAt(1) == slash) {
                /* Drop prefix when child is a UNC pathname */
                childStart = 2;
                
                // 如果child以"\"开头，parent不是"目录相对路径"
            } else if(!isDirectoryRelative) {
                /* Drop prefix when child is drive-relative */
                childStart = 1;
            }
            
            /*
             * childStart
             * 0 - child以"\"开头，且parent是类似"c:"的组成
             * 1 - child以"\"开头，且parent不是类似"c:"的组成
             * 2 - child以"\\"开头
             */
            
            // 如果child只是"\\"，则返回parent去掉末尾'\'的部分
            if(cn == childStart) {
                if(parent.charAt(pn - 1) == slash) {
                    return parent.substring(0, pn - 1);
                }
                
                return parent;
            }
        }
        
        // 忽略parent最后的"\"
        if(parent.charAt(pn - 1) == slash) {
            parentEnd--;
        }
        
        int strlen = parentEnd + cn - childStart;
        char[] theChars = null;
        
        /* 拼接parent和child */
        
        // child以"\"开头，或者parent是类似"c:"的组成
        if(child.charAt(childStart) == slash || isDirectoryRelative) {
            theChars = new char[strlen];
            parent.getChars(0, parentEnd, theChars, 0);
            child.getChars(childStart, cn, theChars, parentEnd);
        } else {
            theChars = new char[strlen + 1];
            parent.getChars(0, parentEnd, theChars, 0);
            
            theChars[parentEnd] = slash;
            
            child.getChars(childStart, cn, theChars, parentEnd + 1);
        }
        
        return new String(theChars);
    }
    
    // 返回默认的父级路径：：Windows系统上是'\'，类Unix系统上是'/'
    @Override
    public String getDefaultParent() {
        return ("" + slash);
    }
    
    // 创建【目录】，返回值指示是否创建成功
    @Override
    public native boolean createDirectory(File dir);
    
    // 创建【文件】，返回值指示是否创建成功
    @Override
    public native boolean createFileExclusively(String path) throws IOException;
    
    // 删除File，如果File是目录，则仅支持删除空目录，返回值指示是否删除成功
    @Override
    public boolean delete(File file) {
        /*
         * Keep canonicalization caches in sync after file deletion and renaming operations.
         * Could be more clever than this (i.e., only remove/update affected entries) but probably
         * not worth it since these entries expire after 30 seconds anyway.
         */
        cache.clear();
        prefixCache.clear();
        return delete0(file);
    }
    
    // 将oldFile重命名为newFile(伴随"移动"的副作用)
    @Override
    public boolean rename(File oldFile, File newFile) {
        /*
         * Keep canonicalization caches in sync after file deletion and renaming operations.
         * Could be more clever than this (i.e., only remove/update affected entries) but probably
         * not worth it since these entries expire after 30 seconds anyway.
         */
        cache.clear();
        prefixCache.clear();
        return rename0(oldFile, newFile);
    }
    
    // 返回子File的路径列表。如果file是文件，返回null。如果file是空目录，返回的数组元素数量也为0。
    @Override
    public native String[] list(File file);
    
    // 返回根目录列表
    @Override
    public File[] listRoots() {
        return BitSet.valueOf(new long[]{listRoots0()}).stream().mapToObj(i -> new File((char) ('A' + i) + ":" + slash)).filter(f -> access(f.getPath()) && f.exists()).toArray(File[]::new);
    }
    
    // 返回File的基础属性
    @Override
    public native int getBooleanAttributes(File file);
    
    // 设置File的最后访问时间
    @Override
    public native boolean setLastModifiedTime(File file, long time);
    
    // 返回File的最后修改时间
    @Override
    public native long getLastModifiedTime(File file);
    
    // 设置File为只读
    @Override
    public native boolean setReadOnly(File file);
    
    // 设置File的访问权限(开启或关闭)
    @Override
    public native boolean setPermission(File file, int access, boolean enable, boolean owneronly);
    
    // 检查File是否具有某个访问属性
    @Override
    public native boolean checkAccess(File file, int access);
    
    // 判断当前本地化路径是否为绝对路径
    @Override
    public boolean isAbsolute(File file) {
        // 返回本地化路径的前缀长度
        int pl = file.getPrefixLength();
        return (((pl == 2) && (file.getPath().charAt(0) == slash)) || (pl == 3));
    }
    
    // 根据flag参数的不同，返回File所在磁盘的大小、磁盘未分配的存储大小、File可用的存储大小
    @Override
    public long getSpace(File file, int flag) {
        if(file.exists()) {
            return getSpace0(file, flag);
        }
        
        return 0;
    }
    
    // 对uri中解析出的path进行后处理，将其转换为普通路径
    @Override
    public String fromURIPath(String path) {
        String p = path;
        
        if((p.length()>2) && (p.charAt(2) == ':')) {
            // "/c:/foo" --> "c:/foo"
            p = p.substring(1);
            
            // "c:/foo/" --> "c:/foo", but "c:/" --> "c:/"
            if((p.length()>3) && p.endsWith("/")) {
                p = p.substring(0, p.length() - 1);
            }
        } else if((p.length()>1) && p.endsWith("/")) {
            // "/foo/" --> "/foo"
            p = p.substring(0, p.length() - 1);
        }
        
        return p;
    }
    
    // 返回对指定路径的规范化形式：盘符大写，解析"."和".."
    @Override
    public String canonicalize(String path) throws IOException {
        // If path is a drive letter only then skip canonicalization
        int len = path.length();
        
        // 对于"c:"类型的路径，转为"C:"后返回
        if((len == 2) && (isLetter(path.charAt(0))) && (path.charAt(1) == ':')) {
            char c = path.charAt(0);
            if((c >= 'A') && (c<='Z')) {
                return path;
            }
            return "" + ((char) (c - 32)) + ':';
            
            // 对于"c:\"类型的路径，转换为"C:\"后返回
        } else if((len == 3) && (isLetter(path.charAt(0))) && (path.charAt(1) == ':') && (path.charAt(2) == '\\')) {
            char c = path.charAt(0);
            if((c >= 'A') && (c<='Z')) {
                return path;
            }
            return "" + ((char) (c - 32)) + ':' + '\\';
        }
        
        if(!useCanonCaches) {
            return canonicalize0(path);
        }
        
        String res = cache.get(path);
        if(res == null) {
            String dir = null;
            String resDir = null;
            
            if(useCanonPrefixCache) {
                dir = parentOrNull(path);
                if(dir != null) {
                    resDir = prefixCache.get(dir);
                    if(resDir != null) {
                        /*
                         * Hit only in prefix cache; full path is canonical,
                         * but we need to get the canonical name of the file
                         * in this directory to get the appropriate
                         * capitalization
                         */
                        String filename = path.substring(1 + dir.length());
                        res = canonicalizeWithPrefix(resDir, filename);
                        cache.put(dir + File.separatorChar + filename, res);
                    }
                }
            }
            
            if(res == null) {
                res = canonicalize0(path);
                cache.put(path, res);
                if(useCanonPrefixCache && dir != null) {
                    resDir = parentOrNull(res);
                    if(resDir != null) {
                        File f = new File(res);
                        if(f.exists() && !f.isDirectory()) {
                            prefixCache.put(dir, resDir);
                        }
                    }
                }
            }
        }
        
        return res;
    }
    
    // 返回File的大小(以字节计)
    @Override
    public native long getLength(File file);
    
    // 返回最大路径名长度
    public int getNameMax(String path) {
        String s = null;
        
        if(path != null) {
            File f = new File(path);
            if(f.isAbsolute()) {
                Path root = f.toPath().getRoot();
                if(root != null) {
                    s = root.toString();
                    if(!s.endsWith("\\")) {
                        s = s + "\\";
                    }
                }
            }
        }
        
        return getNameMax0(s);
    }
    
    /**
     * 比较两个File的本地化路径
     *
     * 当前实现是windows系统，因此会忽略大小写地比较。
     */
    @Override
    public int compare(File f1, File f2) {
        return f1.getPath().compareToIgnoreCase(f2.getPath());
    }
    
    // 计算File的哈希码
    @Override
    public int hashCode(File file) {
        /* Could make this more efficient: String.hashCodeIgnoreCase */
        return file.getPath().toLowerCase(Locale.ENGLISH).hashCode() ^ 1234321;
    }
    
    // 判断字符c是否为路径内部的分隔符(不分系统)
    private boolean isSlash(char c) {
        return (c == '\\') || (c == '/');
    }
    
    // 判断字符c是否为[a-zA-Z]之间的字母
    private boolean isLetter(char c) {
        return ((c >= 'a') && (c<='z')) || ((c >= 'A') && (c<='Z'));
    }
    
    // 如果path不以路径内分隔符开头，则在path开头插入路径内分隔符：'/'或'\'
    private String slashify(String path) {
        if((path.length()>0) && (path.charAt(0) != slash)) {
            return slash + path;
        } else {
            return path;
        }
    }
    
    /**
     * Normalize the given pathname, whose length is len, starting at the given offset;
     * everything before this offset is already normal.
     */
    // 本地化路径：提取磁盘前缀和UNC前缀，将路径分隔符统一为'\'，忽略末尾的路径分隔符
    private String normalize(String path, int len, int off) {
        if(len == 0) {
            return path;
        }
        
        if(off<3) {
            off = 0;   // Avoid fencepost cases with UNC pathnames
        }
        
        int src;
        StringBuilder sb = new StringBuilder(len);
        
        if(off == 0) {
            /*
             * Complete normalization, including prefix
             *
             * 在path的前len个字符中提取规范化的路径前缀。
             *
             * 先尝试提取盘符，如C:，如果不存在，则尝试提取路径内分隔符。
             * 返回已遍历的前缀字符数量。
             */
            src = normalizePrefix(path, len, sb);
        } else {
            /* Partial normalization */
            src = off;
            sb.append(path, 0, off);
        }
        
        /* Remove redundant slashes from the remainder of the path, forcing all slashes into the preferred slash */
        while(src<len) {
            char c = path.charAt(src++);
            
            // 判断字符c是否为路径内部的分隔符(不分系统)
            if(isSlash(c)) {
                // 遍历path，跳过路径内分隔符
                while((src<len) && isSlash(path.charAt(src))) {
                    src++;
                }
                
                // 已经到了路径末尾
                if(src == len) {
                    /* Check for trailing separator */
                    int sn = sb.length();
                    
                    /*
                     * 如果解析出的路径仅包含盘符，则为其添加路径内分隔符
                     *
                     * 如将"z:"转换为"z:\\"
                     */
                    if((sn == 2) && (sb.charAt(1) == ':')) {
                        /* "z:\\" */
                        sb.append(slash);
                        break;
                    }
                    
                    // 如果path中仅有一个路径内分隔符，则将其解析为"\"后返回
                    if(sn == 0) {
                        /* "\\" */
                        sb.append(slash);
                        break;
                    }
                    
                    // 如果path仅包含多个连续的路径内分隔符，则将其解析为"\\"后返回
                    if((sn == 1) && (isSlash(sb.charAt(0)))) {
                        /*
                         * "\\\\" is not collapsed to "\\" because "\\\\" marks the beginning of a UNC pathname.
                         * Even though it is not, by itself, a valid UNC pathname,
                         * we leave it as is in order to be consistent with the win32 APIs,
                         * which treat this case as an invalid UNC pathname rather than as an alias
                         * for the root directory of the current drive.
                         */
                        sb.append(slash);
                        break;
                    }
                    
                    /* Path does not denote a root directory, so do not append trailing slash */
                    break;
                } else {
                    // 压缩连续的路径内分隔符(合并为一个'\'后追加)
                    sb.append(slash);
                }
            } else {
                // 普通字符，直接追加
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * A normal Win32 pathname contains no duplicate slashes,
     * except possibly for a UNC prefix, and does not end with a slash.
     * It may be the empty string.
     * Normalized Win32 pathnames have the convenient property that the length of the prefix
     * almost uniquely identifies the type of the path and whether it is absolute or relative:
     * 0  relative to both drive and directory
     * 1  drive-relative (begins with '\\')
     * 2  absolute UNC (if first char is '\\'), else directory-relative (has form "z:foo")
     * 3  absolute local pathname (begins with "z:\\")
     */
    /*
     * 在path的前len个字符中提取本地化的路径前缀。
     *
     * 先尝试提取盘符，如C:，如果不存在，则尝试提取路径内分隔符。
     * 返回已遍历的前缀字符数量。
     */
    private int normalizePrefix(String path, int len, StringBuilder sb) {
        int src = 0;
        
        // 遍历path，跳过路径内分隔符
        while((src<len) && isSlash(path.charAt(src))) {
            src++;
        }
        
        char c;
        
        // 提取出盘符
        if((len - src >= 2) && isLetter(c = path.charAt(src)) && path.charAt(src + 1) == ':') {
            /*
             * Remove leading slashes if followed by drive specifier.
             * This hack is necessary to support file URLs containing drive specifiers (e.g., "file://c:/path").
             * As a side effect, "/c:/path" can be used as an alternative to "c:/path".
             */
            sb.append(c);
            sb.append(':');
            src += 2;
        } else {
            src = 0;
            
            // 如果开头两个字符是路径内分隔符，则将其识别为UNC路径
            if((len >= 2) && isSlash(path.charAt(0)) && isSlash(path.charAt(1))) {
                /*
                 * UNC pathname:
                 * Retain first slash; leave src pointed at second slash so that further slashes will be collapsed into the second slash.
                 * The result will be a pathname beginning with "\\\\" followed (most likely) by a host name.
                 */
                src = 1;
                sb.append(slash);
            }
        }
        
        return src;
    }
    
    /**
     * Best-effort attempt to get parent of this path;
     * used for optimization of filename canonicalization.
     * This must return null for any cases where the code in canonicalize_md.c would throw an exception
     * or otherwise deal with non-simple pathnames like handling of "." and "..".
     * It may conservatively return null in other situations as well.
     * Returning null will cause the underlying (expensive) canonicalization routine to be called.
     */
    private static String parentOrNull(String path) {
        if(path == null) {
            return null;
        }
        
        char sep = File.separatorChar;
        char altSep = '/';
        int last = path.length() - 1;
        int idx = last;
        int adjacentDots = 0;
        int nonDotCount = 0;
        while(idx>0) {
            char c = path.charAt(idx);
            if(c == '.') {
                if(++adjacentDots >= 2) {
                    // Punt on pathnames containing . and ..
                    return null;
                }
                
                if(nonDotCount == 0) {
                    // Punt on pathnames ending in a .
                    return null;
                }
            } else if(c == sep) {
                if(adjacentDots == 1 && nonDotCount == 0) {
                    // Punt on pathnames containing . and ..
                    return null;
                }
                if(idx == 0 || idx >= last - 1 || path.charAt(idx - 1) == sep || path.charAt(idx - 1) == altSep) {
                    // Punt on pathnames containing adjacent slashes
                    // toward the end
                    return null;
                }
                return path.substring(0, idx);
            } else if(c == altSep) {
                // Punt on pathnames containing both backward and
                // forward slashes
                return null;
            } else if(c == '*' || c == '?') {
                // Punt on pathnames containing wildcards
                return null;
            } else {
                ++nonDotCount;
                adjacentDots = 0;
            }
            --idx;
        }
        return null;
    }
    
    // 返回用户工作目录[user.dir]
    private String getUserPath() {
        /* For both compatibility and security, we must look this up every time */
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPropertyAccess("user.dir");
        }
        
        return normalize(userDir);
    }
    
    // 返回绝对路径中的盘符信息
    private String getDrive(String path) {
        int pl = prefixLength(path);
        return (pl == 3) ? path.substring(0, 2) : null;
    }
    
    // 获取指定的盘符对应的相对目录信息
    private native String getDriveDirectory(int drive);
    
    /*
     * 获取指定的盘符对应的相对目录信息
     *
     * 假设[user.dir]为"C:\Users\kang\Desktop\test"
     * 那么对于'c'，返回"\Users\kang\Desktop\test"
     * 而对于'd'，仅返回"\"
     */
    private String getDriveDirectory(char drive) {
        // 获取指定的盘符在字母表中的位置
        int i = driveIndex(drive);
        if(i<0) {
            return null;
        }
        
        String s = driveDirCache[i];
        if(s != null) {
            return s;
        }
        
        // 获取指定的盘符对应的目录信息，如3对应"C:\"，4对应"D:\"
        s = getDriveDirectory(i + 1);
        
        driveDirCache[i] = s;
        
        return s;
    }
    
    // 返回指定的盘符在字母表中的位置
    private static int driveIndex(char drive) {
        if((drive >= 'a') && (drive<='z')) {
            return drive - 'a';
        }
        
        if((drive >= 'A') && (drive<='Z')) {
            return drive - 'A';
        }
        
        return -1;
    }
    
    private native String canonicalize0(String path) throws IOException;
    
    private String canonicalizeWithPrefix(String canonicalPrefix, String filename) throws IOException {
        return canonicalizeWithPrefix0(canonicalPrefix, canonicalPrefix + File.separatorChar + filename);
    }
    
    /**
     * Run the canonicalization operation assuming that the prefix
     * (everything up to the last filename) is canonical;
     * just gets the canonical name of the last element of the path
     */
    private native String canonicalizeWithPrefix0(String canonicalPrefix, String pathWithCanonicalPrefix) throws IOException;
    
    private native boolean delete0(File file);
    
    // 将oldFile重命名为newFile
    private native boolean rename0(File oldFile, File newFile);
    
    private static native int listRoots0();
    
    private boolean access(String path) {
        try {
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkRead(path);
            }
            return true;
        } catch(SecurityException x) {
            return false;
        }
    }
    
    private native long getSpace0(File file, int t);
    
    /**
     * Obtain maximum file component length from GetVolumeInformation which
     * expects the path to be null or a root component ending in a backslash
     */
    private native int getNameMax0(String path);
    
    private static native void initIDs();
    
}
