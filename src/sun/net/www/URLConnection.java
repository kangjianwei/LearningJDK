/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * A class to represent an active connection to an object represented by a URL.
 *
 * @author James Gosling
 */
/*
 * URL连接，是以下四种资源连接的抽象父类
 * 1.FileURLConnection
 * 2.FtpURLConnection
 * 3.MailToURLConnection
 * 4.JavaRuntimeURLConnection
 */
public abstract class URLConnection extends java.net.URLConnection {
    
    private static HashMap<String, Void> proxiedHosts = new HashMap<>();
    
    // 当前协议下的MIME信息头
    protected MessageHeader properties;
    
    // "content-type"
    private String contentType;
    
    // "content-length"
    private int contentLength = -1;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a URLConnection object.  These should not be created directly:
     * instead they should be created by protocol handers in response to URL.openConnection.
     *
     * @param u The URL that this connects to.
     */
    public URLConnection(URL url) {
        super(url);
        properties = new MessageHeader();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 请求头 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 设置请求头，此处被覆盖实现为设置MIME信息头
    public void setRequestProperty(String key, String value) {
        if(connected) {
            throw new IllegalAccessError("Already connected");
        }
        
        if(key == null) {
            throw new NullPointerException("key cannot be null");
        }
        
        properties.set(key, value);
    }
    
    /**
     * The following three methods addRequestProperty, getRequestProperty,
     * and getRequestProperties were copied from the superclass implementation
     * before it was changed by CR:6230836, to maintain backward compatibility.
     */
    // 添加请求头，此处未对该方法进行实现，只是为了兼容而保留该方法
    public void addRequestProperty(String key, String value) {
        if(connected) {
            throw new IllegalStateException("Already connected");
        }
        
        if(key == null) {
            throw new NullPointerException("key is null");
        }
    }
    
    // 获取请求头，此处被覆盖实现为简单地返回空集，只是为了兼容而保留该方法
    public Map<String, List<String>> getRequestProperties() {
        if(connected) {
            throw new IllegalStateException("Already connected");
        }
        
        return Collections.emptyMap();
    }
    
    // 获取指定key的请求头，此处被覆盖实现为简单地返回null，只是为了兼容而保留该方法
    public String getRequestProperty(String key) {
        if(connected) {
            throw new IllegalStateException("Already connected");
        }
        
        return null;
    }
    
    /*▲ 请求头 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 头信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Call this routine to set the property list for this object. */
    // 设置MIME信息头
    public void setProperties(MessageHeader properties) {
        this.properties = properties;
    }
    
    /**
     * Call this routine to get the property list for this object.
     * Properties (like content-type) that have explicit getXX() methods
     * associated with them should be accessed using those methods.
     */
    // 返回MIME信息头
    public MessageHeader getProperties() {
        return properties;
    }
    
    // 获取指定名称的MIME信息头的值
    public String getHeaderField(String name) {
        try {
            getInputStream();
        } catch(Exception e) {
            return null;
        }
        
        return properties == null ? null : properties.findValue(name);
    }
    
    /**
     * Return the key for the nth header field.
     * Returns null if there are fewer than n fields.
     * This can be used to iterate through all the headers in the message.
     */
    // 返回第n(>=0)条MIME信息头的key
    public String getHeaderFieldKey(int n) {
        try {
            getInputStream();
        } catch(Exception e) {
            return null;
        }
    
        return properties == null ? null : properties.getKey(n);
    }
    
    /**
     * Return the value for the nth header field.
     * Returns null if there are fewer than n fields.
     * This can be used in conjunction with getHeaderFieldKey to iterate through all the headers in the message.
     */
    // 返回第n(>=0)条MIME信息头的value
    public String getHeaderField(int n) {
        try {
            getInputStream();
        } catch(Exception e) {
            return null;
        }
        
        return properties == null ? null : properties.getValue(n);
    }
    
    /*▲ 头信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 消息头 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Call this routine to get the content-type associated with this object.
     */
    // 返回"content-type"
    public String getContentType() {
        if(contentType == null) {
            // 获取MIME信息头中设定的"content-type"值
            contentType = getHeaderField("content-type");
            if(contentType != null) {
                return contentType;
            }
        }
        
        String ct = null;
        try {
            // 返回指向当前URL资源的输入流，可以从中读取数据
            InputStream is = getInputStream();
            
            // 对于特定类型的文件，可以通过解析其输入流的前几个字节来猜测该文件的真实类型
            ct = guessContentTypeFromStream(is);
        } catch(java.io.IOException e) {
        }
        
        if(ct == null) {
            // 获取MIME信息头中设定的"content-type"值
            ct = properties.findValue("content-type");
            
            if(ct == null) {
                // 以"/"结尾被认为是普通网页
                if(url.getFile().endsWith("/")) {
                    ct = "text/html";
                } else {
                    // 继续猜测，这里通常是通过后缀猜测
                    ct = guessContentTypeFromName(url.getFile());
                }
            }
        }
        
        /*
         * If the Mime header had a Content-encoding field and its value
         * was not one of the values that essentially indicate no
         * encoding, we force the content type to be unknown. This will
         * cause a save dialog to be presented to the user.  It is not
         * ideal but is better than what we were previously doing, namely
         * bringing up an image tool for compressed tar files.
         */
        
        
        // 获取MIME信息头中设定的"content-encoding"值
        String ce = properties.findValue("content-encoding");
        
        if(ct == null || ce != null && !ce.equalsIgnoreCase("7bit") && !ce.equalsIgnoreCase("8bit") && !ce.equalsIgnoreCase("binary")) {
            ct = "content/unknown";
        }
        
        setContentType(ct);
        
        return contentType;
    }
    
    /**
     * Set the content type of this URL to a specific value.
     *
     * @param type The content type to use.  One of the
     *             content_* static variables in this
     *             class should be used.
     *             eg. setType(URL.content_html);
     */
    // 设置"content-type"
    public void setContentType(String type) {
        contentType = type;
        properties.set("content-type", type);
    }
    
    
    /**
     * Call this routine to get the content-length associated with this
     * object.
     */
    // 返回"content-length"
    public int getContentLength() {
        try {
            getInputStream();
        } catch(Exception e) {
            return -1;
        }
        
        int l = contentLength;
        if(l<0) {
            try {
                l = Integer.parseInt(properties.findValue("content-length"));
                setContentLength(l);
            } catch(Exception e) {
            }
        }
        return l;
    }
    
    /**
     * Call this routine to set the content-length associated with this
     * object.
     */
    // 设置"content-length"
    protected void setContentLength(int length) {
        contentLength = length;
        properties.set("content-length", String.valueOf(length));
    }
    
    /*▲ 消息头 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public static synchronized void setProxiedHost(String host) {
        proxiedHosts.put(host.toLowerCase(), null);
    }
    
    public static synchronized boolean isProxiedHost(String host) {
        return proxiedHosts.containsKey(host.toLowerCase());
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns true if the data associated with this URL can be cached.
     */
    // 判断当前URL是否可以被缓存
    public boolean canCache() {
        return url.getFile().indexOf('?')<0   /* && url.postData == null REMIND */;
    }
    
    /**
     * Call this to close the connection and flush any remaining data.
     * Overriders must remember to call super.close()
     */
    public void close() {
        url = null;
    }
    
}
