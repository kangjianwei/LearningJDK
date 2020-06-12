/*
 * Copyright (c) 1995, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.file;

import java.net.URL;
import java.net.FileNameMap;
import java.io.*;
import java.text.Collator;
import java.security.Permission;
import sun.net.*;
import sun.net.www.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Open an file input stream given a URL.
 *
 * @author James Gosling
 * @author Steven B. Byrne
 */
// "file"协议下对应的URL连接
public class FileURLConnection extends URLConnection {
    
    static String CONTENT_TYPE = "content-type";
    static String TEXT_PLAIN = "text/plain";
    static String CONTENT_LENGTH = "content-length";
    static String LAST_MODIFIED = "last-modified";
    
    File file;          // URL指向的资源
    List<String> files; // 如果file是目录，fiels是所有子目录/文件的名称
    
    String filename;    // 指file名称
    boolean isDirectory = false;    // 指示file是否为目录
    
    boolean exists = false;         // 指示当前资源是否存在
    
    InputStream is;     // 如果file是文件，则is指向其输入流
    
    String contentType;     // File协议Head信息：file内容类型，对于目录是"text/plain"
    long length = -1;       // File协议Head信息：file长度
    long lastModified = 0;  // File协议Head信息：file最后修改时间
    
    private boolean initializedHeaders = false; // 当前协议下的Head信息是否已初始化
    
    Permission permission;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected FileURLConnection(URL url, File file) {
        super(url);
        this.file = file;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 连接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Note: the semantics of FileURLConnection object is that the
     * results of the various URLConnection calls, such as
     * getContentType, getInputStream or getContentLength reflect
     * whatever was true when connect was called.
     */
    // 连接到当前URL指向的资源
    public void connect() throws IOException {
        if(connected) {
            return;
        }
        
        try {
            filename = file.toString();
            isDirectory = file.isDirectory();
            
            if(isDirectory) {
                String[] fileList = file.list();
                if(fileList == null) {
                    throw new FileNotFoundException(filename + " exists, but is not accessible");
                }
                files = Arrays.asList(fileList);
            } else {
                is = new BufferedInputStream(new FileInputStream(filename));
                
                // Check if URL should be metered
                boolean meteredInput = ProgressMonitor.getDefault().shouldMeterInput(url, "GET");
                if(meteredInput) {
                    ProgressSource pi = new ProgressSource(url, "GET", file.length());
                    is = new MeteredStream(is, pi, file.length());
                }
            }
        } catch(IOException e) {
            throw e;
        }
        
        connected = true;
    }
    
    /*▲ 连接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回指向URL资源的输入流，可以从中读取数据
    public synchronized InputStream getInputStream() throws IOException {
        
        int iconHeight;
        int iconWidth;
        
        // 连接到当前URL指定的资源
        connect();
        
        // 如果file是文件，则is有效，此处直接返回
        if(is != null) {
            return is;
        }
        
        // 如果file不是文件，则只能是目录
        if(!isDirectory) {
            throw new FileNotFoundException(filename);
        }
        
        // 获取文件名到MIME类型的映射
        FileNameMap map = java.net.URLConnection.getFileNameMap();
        
        // 存储子file的名称
        StringBuilder sb = new StringBuilder();
        
        if(files == null) {
            throw new FileNotFoundException(filename);
        }
        
        // 对子文件/子目录进行自然排序
        files.sort(Collator.getInstance());
        
        for(String fileName : files) {
            sb.append(fileName);
            sb.append("\n");
        }
        
        // Put it into a (default) locale-specific byte-stream.
        is = new ByteArrayInputStream(sb.toString().getBytes());
        
        // 如果file是目录，此处返回的流中包含了子文件/子目录的名称
        return is;
    }
    
    /*▲ 字节流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 头信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回MIME信息头
    public MessageHeader getProperties() {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return super.getProperties();
    }
    
    // 获取指定名称的MIME信息头的值
    public String getHeaderField(String name) {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return super.getHeaderField(name);
    }
    
    // 返回第n(>=0)条MIME信息头的value
    public String getHeaderField(int n) {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return super.getHeaderField(n);
    }
    
    // 返回第n(>=0)条MIME信息头的key
    public String getHeaderFieldKey(int n) {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return super.getHeaderFieldKey(n);
    }
    
    // 初始化当前协议下的MIME信息头
    private void initializeHeaders() {
        try {
            // 连接到当前URL指定的资源
            connect();
            // 当前资源是否存在
            exists = file.exists();
        } catch(IOException e) {
        }
        
        if(initializedHeaders && exists) {
            return;
        }
        
        // 返回File的大小(以字节计)
        length = file.length();
        
        // 返回File的最后修改时间
        lastModified = file.lastModified();
        
        // 如果当前资源是目录
        if(isDirectory) {
            // 记录content-type="text/plain"
            properties.add(CONTENT_TYPE, TEXT_PLAIN);
        } else {
            // 获取文件名到MIME类型的映射
            FileNameMap map = java.net.URLConnection.getFileNameMap();
            
            // 尝试从map中获取到content-type的值
            contentType = map.getContentTypeFor(filename);
            if(contentType != null) {
                // 记录"content-type"
                properties.add(CONTENT_TYPE, contentType);
            }
            
            // 记录"content-length"
            properties.add(CONTENT_LENGTH, String.valueOf(length));
            
            /*
             * Format the last-modified field into the preferred Internet standard
             * - ie: fixed-length subset of that defined by RFC 1123
             */
            // 格式化lastModified
            if(lastModified != 0) {
                Date date = new Date(lastModified);
                SimpleDateFormat fo = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                fo.setTimeZone(TimeZone.getTimeZone("GMT"));
                
                // 记录"last-modified"
                properties.add(LAST_MODIFIED, fo.format(date));
            }
        }
        
        initializedHeaders = true;
    }
    
    /*▲ 头信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 消息头 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回"content-length"
    public int getContentLength() {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        if(length>Integer.MAX_VALUE) {
            return -1;
        }
        return (int) length;
    }
    
    // 返回"content-length"，以long形式返回
    public long getContentLengthLong() {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return length;
    }
    
    // 返回"last-modified"
    public long getLastModified() {
        // 初始化当前协议下的Head信息
        initializeHeaders();
        return lastModified;
    }
    
    /*▲ 消息头 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * since getOutputStream isn't supported, only read permission is relevant
     */
    public Permission getPermission() throws IOException {
        if(permission != null) {
            return permission;
        }
        
        String decodedPath = ParseUtil.decode(url.getPath());
        if(File.separatorChar == '/') {
            permission = new FilePermission(decodedPath, "read");
        } else {
            // decode could return /c:/x/y/z.
            if(decodedPath.length()>2 && decodedPath.charAt(0) == '/' && decodedPath.charAt(2) == ':') {
                decodedPath = decodedPath.substring(1);
            }
            permission = new FilePermission(decodedPath.replace('/', File.separatorChar), "read");
        }
        
        return permission;
    }
    
}
