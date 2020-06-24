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

import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Locale;
import java.io.IOException;

/**
 * Base implementation of FileTypeDetector
 */
// 文件类型检测器的抽象实现，更进一步的实现取决于不同的平台
public abstract class AbstractFileTypeDetector extends FileTypeDetector {
    
    /**
     * Special characters
     */
    // content type中排除的字符
    private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";
    
    protected AbstractFileTypeDetector() {
        super();
    }
    
    /**
     * Invokes the appropriate probe method to guess a file's content type,
     * and checks that the content type's syntax is valid.
     */
    // 返回指定文件的类型(Content-Type)
    @Override
    public final String probeContentType(Path file) throws IOException {
        if(file == null) {
            throw new NullPointerException("'file' is null");
        }
        
        // 获取指定文件的类型(Content-Type)，不同的平台有各自的实现
        String result = implProbeContentType(file);
        
        // 如果上面的操作不成功，则回退到使用系统内置的MIME映射来判断文件类型
        if(result == null) {
            // 获取当前路径的名称(路径上最后一个组件)
            Path fileName = file.getFileName();
            if(fileName != null) {
                // 获取文件名到MIME类型的映射
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                // 获取指定文件名对应的MIME类型
                result = fileNameMap.getContentTypeFor(fileName.toString());
            }
        }
        
        // 如果result不为null，则从result中解析出首个匹配的content type
        return (result == null) ? null : parse(result);
    }
    
    /**
     * Probes the given file to guess its content type.
     */
    // 获取指定文件的类型(Content-Type)，不同的平台有各自的实现
    protected abstract String implProbeContentType(Path file) throws IOException;
    
    /**
     * Returns the extension of a file name, specifically the portion of the
     * parameter string after the first dot. If the parameter is {@code null},
     * empty, does not contain a dot, or the dot is the last character, then an
     * empty string is returned, otherwise the characters after the dot are
     * returned.
     *
     * @param name A file name
     *
     * @return The characters after the first dot or an empty string.
     */
    // 从指定的文件名中提取后缀
    protected final String getExtension(String name) {
        String ext = "";
    
        if(name != null && !name.isEmpty()) {
            int dot = name.indexOf('.');
            if((dot >= 0) && (dot<name.length() - 1)) {
                ext = name.substring(dot + 1);
            }
        }
    
        return ext;
    }
    
    /**
     * Parses a candidate content type into its type and subtype, returning null if either token is invalid.
     */
    // 从指定的字符串中解析出首个匹配的content type
    private static String parse(String s) {
        // 获取'/'的位置
        int slash = s.indexOf('/');
        if(slash<0) {
            return null;  // no subtype
        }
    
        // 获取content type中'/'之前的部分
        String type = s.substring(0, slash).trim().toLowerCase(Locale.ENGLISH);
        if(!isValidToken(type)) {
            return null;  // invalid type
        }
    
        // 获取';'的位置
        int semicolon = s.indexOf(';');
        // 如果存在多个subtype，只取第一个
        String subtype = (semicolon<0) ? s.substring(slash + 1) : s.substring(slash + 1, semicolon);
        subtype = subtype.trim().toLowerCase(Locale.ENGLISH);
        if(!isValidToken(subtype)) {
            return null;  // invalid subtype
        }
    
        return type + '/' + subtype;
    }
    
    /**
     * Returns true if the given string is a legal type or subtype.
     */
    // 判断指定的字符串是否可以出现在content type中
    private static boolean isValidToken(String s) {
        int len = s.length();
        if(len == 0) {
            return false;
        }
    
        for(int i = 0; i<len; i++) {
            if(!isTokenChar(s.charAt(i))) {
                return false;
            }
        }
    
        return true;
    }
    
    /**
     * Returns true if the character is a valid token character.
     */
    // 判断指定的字符是否可以出现在content type中
    private static boolean isTokenChar(char c) {
        return (c>32) && (c<127) && (TSPECIALS.indexOf(c)<0);
    }
    
}
