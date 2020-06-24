/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.InvalidPathException;

/**
 * A parser of Windows path strings
 */
// windows路径解析器
class WindowsPathParser {
    
    // Reserved characters for window path name
    private static final String reservedChars = "<>:\"|?*"; // windows路径中的非法字符
    
    private WindowsPathParser() {
    }
    
    /**
     * Parses the given input as a Windows path
     */
    // 对指定的路径input进行解析，需要对解析后的路径本地化
    static Result parse(String input) {
        return parse(input, true);
    }
    
    /**
     * Parses the given input as a Windows path where it is known that the
     * path is already normalized.
     */
    // 对指定的路径input进行解析，不会对解析后的路径本地化
    static Result parseNormalizedPath(String input) {
        return parse(input, false);
    }
    
    /**
     * Parses the given input as a Windows path.
     *
     * @param requireToNormalize Indicates if the path requires to be normalized
     */
    // 对指定的路径input进行解析，requireToNormalize指示是否将解析后的路径本地化
    private static Result parse(String input, boolean requireToNormalize) {
        String root = "";
    
        WindowsPathType type = null;
    
        int len = input.length();
    
        int off = 0;
    
        // 如果路径不为空
        if(len>1) {
            char c0 = input.charAt(0);  // 第一个字符
            char c1 = input.charAt(1);  // 第二个字符
            char c = 0;
            int next = 2;
        
            // 如果路径以两个路径分隔符开头，按UNC路径解析
            if(isSlash(c0) && isSlash(c1)) {
                /*
                 * UNC: We keep the first two slash, collapse all the following,
                 * then take the hostname and share name out,
                 * meanwhile collapsing all the redundant slashes.
                 */
                type = WindowsPathType.UNC;
            
                // 返回input的[next, len)范围内首个【非】路径分隔符的索引
                off = nextNonSlash(input, next, len);
            
                // 返回input的[off, len)范围内首个路径分隔符的索引
                next = nextSlash(input, off, len);
                if(off == next) {
                    throw new InvalidPathException(input, "UNC path is missing hostname");
                }
            
                // 获取host名称
                String host = input.substring(off, next);
            
                // 返回input的[next, len)范围内首个【非】路径分隔符的索引
                off = nextNonSlash(input, next, len);
            
                // 返回input的[off, len)范围内首个路径分隔符的索引
                next = nextSlash(input, off, len);
                if(off == next) {
                    throw new InvalidPathException(input, "UNC path is missing sharename");
                }
            
                root = "\\\\" + host + "\\" + input.substring(off, next) + "\\";
            
                off = next;
            } else {
                // 路径以类似"C:"这种格式开头
                if(isLetter(c0) && c1 == ':') {
                    char c2;
                
                    if(len>2 && isSlash(c2 = input.charAt(2))) {
                        // avoid concatenation when root is "D:\"
                        if(c2 == '\\') {
                            root = input.substring(0, 3);
                        } else {
                            root = input.substring(0, 2) + '\\';
                        }
                        off = 3;
                        type = WindowsPathType.ABSOLUTE;
                    } else {
                        root = input.substring(0, 2);
                        off = 2;
                        type = WindowsPathType.DRIVE_RELATIVE;
                    }
                }
            }
        }
    
        if(off == 0) {
            if(len>0 && isSlash(input.charAt(0))) {
                type = WindowsPathType.DIRECTORY_RELATIVE;
                root = "\\";
            } else {
                type = WindowsPathType.RELATIVE;
            }
        }
    
        // 如果需要将路径本地化
        if(requireToNormalize) {
            StringBuilder sb = new StringBuilder(input.length());
            sb.append(root);
        
            // 将指定的路径本地化
            String path = normalize(sb, input, off);
        
            return new Result(type, root, path);
        } else {
            return new Result(type, root, input);
        }
    }
    
    /**
     * Remove redundant slashes from the rest of the path, forcing all slashes into the preferred slash.
     */
    // 将指定的路径本地化（移除多余路径分隔符，并将'/'转为'\'）
    private static String normalize(StringBuilder sb, String path, int off) {
        int len = path.length();
        
        // 返回指定路径的[off, end)范围内首个【非】路径分隔符的索引
        off = nextNonSlash(path, off, len);
        
        int start = off;
        
        char lastC = 0;
        
        while(off<len) {
            char c = path.charAt(off);
            
            if(isSlash(c)) {
                if(lastC == ' ') {
                    throw new InvalidPathException(path, "Trailing char <" + lastC + ">", off - 1);
                }
                
                // 向sb中添加[start, off)范围的字符序列
                sb.append(path, start, off);
                
                // 返回指定路径的[off, end)范围内首个【非】路径分隔符的索引
                off = nextNonSlash(path, off, len);
                
                // no slash at the end of normalized path
                if(off != len) {
                    sb.append('\\');
                }
                
                start = off;
            } else {
                // 如果字符c是非法的windows路径字符
                if(isInvalidPathChar(c)) {
                    throw new InvalidPathException(path, "Illegal char <" + c + ">", off);
                }
                
                lastC = c;
                off++;
            }
        }
        
        if(start != off) {
            if(lastC == ' ') {
                throw new InvalidPathException(path, "Trailing char <" + lastC + ">", off - 1);
            }
            
            sb.append(path, start, off);
        }
        
        return sb.toString();
    }
    
    // 返回指定路径的[off, end)范围内首个【非】路径分隔符的索引
    private static final int nextNonSlash(String path, int off, int end) {
        while(off<end && isSlash(path.charAt(off))) {
            off++;
        }
        
        return off;
    }
    
    // 返回指定路径的[off, end)范围内首个路径分隔符的索引
    private static final int nextSlash(String path, int off, int end) {
        char c;
        
        while(off<end && !isSlash(c = path.charAt(off))) {
            // 如果字符c是非法的windows路径字符
            if(isInvalidPathChar(c)) {
                throw new InvalidPathException(path, "Illegal character [" + c + "] in path", off);
            }
            
            off++;
        }
        
        return off;
    }
    
    // 判断字符c是否为路径内分隔符'/'或'\'
    private static final boolean isSlash(char c) {
        return (c == '\\') || (c == '/');
    }
    
    // 判断字符c是否为字母[a-zA-Z]
    private static final boolean isLetter(char c) {
        return ((c >= 'a') && (c<='z')) || ((c >= 'A') && (c<='Z'));
    }
    
    // 判断字符c是否为非法的windows路径字符
    private static final boolean isInvalidPathChar(char c) {
        return c<'\u0020' || reservedChars.indexOf(c) != -1;
    }
    
    /**
     * The result of a parse operation
     */
    // 路径解析的结果
    static class Result {
        private final WindowsPathType type;
        private final String root;
        private final String path;
        
        Result(WindowsPathType type, String root, String path) {
            this.type = type;
            this.root = root;
            this.path = path;
        }
        
        /**
         * The path type
         */
        WindowsPathType type() {
            return type;
        }
        
        /**
         * The root component
         */
        String root() {
            return root;
        }
        
        /**
         * The normalized path (includes root)
         */
        String path() {
            return path;
        }
    }
    
}
