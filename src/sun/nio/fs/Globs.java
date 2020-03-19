/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.PatternSyntaxException;

/*
 * "glob"正则，此处用来匹配文件名，可以转换为普通的正则
 *
 * "glob"正则的语法简述如下：
 *
 *      **：和*一样，可以匹配路径中的 0个 或 多个，而且**可以匹配当前目录和子目录。但无法抓去符号链接的目录。
 *      *            匹配除斜杠之外的所有内容。在windows上，它将避免匹配反斜杠
 *      **           匹配零个或多个目录，但永远不匹配目录.和..
 *      ?            匹配任意单个字符
 *      [seq]        匹配seq中的任何字符
 *      [!seq]       匹配除seq中的任何字符
 *      [[:alnum:]]  序列中的POSIX样式字符类
 *      \            转义字符。如果应用于元字符，它将被视为普通字符
 *      !            排除模式
 *      {}           捕获分组
 *
 * 注：这里仅支持部分"glob"正则符号，更多描述参见：https://facelessuser.github.io/wcmatch/glob/
 */
public class Globs {
    private static final String regexMetaChars = ".^$+{[]|()";  // 普通正则符号(部分)
    private static final String globMetaChars = "\\*?[{";       // "glob"正则符号(部分)
    
    private static char EOL = 0;  // TBD
    
    private Globs() {
    }
    
    // 将"glob"正则转换为类unix系统上的普通正则
    static String toUnixRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, false);
    }
    
    // 将"glob"正则转换为windows系统上的普通正则
    static String toWindowsRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, true);
    }
    
    // 判断字符c是否为普通正则符号
    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf(c) != -1;
    }
    
    // 判断字符c是否为"glob"正则符号
    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf(c) != -1;
    }
    
    // 获取glob[i]
    private static char next(String glob, int i) {
        if(i<glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }
    
    /**
     * Creates a regex pattern from the given glob expression.
     *
     * @throws PatternSyntaxException
     */
    // 将"glob"正则转换为普通正则，isDos决定将其转换为windows正则(true)还是unix正则(false)
    private static String toRegexPattern(String globPattern, boolean isDos) {
        boolean inGroup = false;
        
        // 普通正则符号，用"^"作为字符串起始或行的起始
        StringBuilder regex = new StringBuilder("^");
        
        int i = 0;
        while(i<globPattern.length()) {
            // 逐个取出"glob"正则中的符号
            char c = globPattern.charAt(i++);
            
            switch(c) {
                // 转义符号
                case '\\':
                    // escape special characters
                    if(i == globPattern.length()) {
                        throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
                    }
                    
                    // 获取下一个符号
                    char next = globPattern.charAt(i++);
                    
                    // 如果next仍然是预设的正则符号
                    if(isGlobMeta(next) || isRegexMeta(next)) {
                        // 添加"\"
                        regex.append('\\');
                    }
                    
                    // 添加待转义符号
                    regex.append(next);
                    break;
                
                // 路径标记
                case '/':
                    // 如果需要转换为windows系统下的路径，需要添加"\\"
                    if(isDos) {
                        regex.append("\\\\");
                        
                        // 如果需要转换为unix系统下的路径，则直接添加"/"
                    } else {
                        regex.append(c);
                    }
                    break;
                
                case '[':
                    // don't match name separator in class
                    if(isDos) {
                        regex.append("[[^\\\\]&&[");
                    } else {
                        regex.append("[[^/]&&[");
                    }
                    
                    if(next(globPattern, i) == '^') {
                        // escape the regex negation char if it appears
                        regex.append("\\^");
                        i++;
                    } else {
                        // negation
                        if(next(globPattern, i) == '!') {
                            regex.append('^');
                            i++;
                        }
                        
                        // hyphen allowed at start
                        if(next(globPattern, i) == '-') {
                            regex.append('-');
                            i++;
                        }
                    }
                    
                    boolean hasRangeStart = false;
                    char last = 0;
                    while(i<globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if(c == ']') {
                            break;
                        }
                        
                        if(c == '/' || (isDos && c == '\\')) {
                            throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern, i - 1);
                        }
                        
                        // TBD: how to specify ']' in a class?
                        if(c == '\\' || c == '[' || c == '&' && next(globPattern, i) == '&') {
                            // escape '\', '[' or "&&" for regex class
                            regex.append('\\');
                        }
                        
                        regex.append(c);
                        
                        if(c == '-') {
                            if(!hasRangeStart) {
                                throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
                            }
                            
                            if((c = next(globPattern, i++)) == EOL || c == ']') {
                                break;
                            }
                            
                            if(c<last) {
                                throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
                            }
                            
                            regex.append(c);
                            
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    
                    if(c != ']') {
                        throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
                    }
                    
                    regex.append("]]");
                    break;
                
                case '{':
                    if(inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                
                case '}':
                    if(inGroup) {
                        regex.append("))");
                        inGroup = false;
                    } else {
                        regex.append('}');
                    }
                    break;
                
                case ',':
                    if(inGroup) {
                        regex.append(")|(?:");
                    } else {
                        regex.append(',');
                    }
                    break;
                
                case '*':
                    if(next(globPattern, i) == '*') {
                        // crosses directory boundaries
                        regex.append(".*");
                        i++;
                    } else {
                        // within directory boundary
                        if(isDos) {
                            regex.append("[^\\\\]*");
                        } else {
                            regex.append("[^/]*");
                        }
                    }
                    break;
                
                case '?':
                    if(isDos) {
                        regex.append("[^\\\\]");
                    } else {
                        regex.append("[^/]");
                    }
                    break;
                
                default:
                    if(isRegexMeta(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
            }
        }
        
        if(inGroup) {
            throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
        }
        
        return regex.append('$').toString();
    }
}
