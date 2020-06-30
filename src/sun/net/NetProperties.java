/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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
package sun.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import jdk.internal.util.StaticProperty;

/**
 * This class allows for centralized access to Networking properties.
 * Default values are loaded from the file jre/lib/net.properties
 *
 * @author Jean-Christophe Collet
 */
// 网络属性配置，默认从jre/lib/net.properties中加载
public class NetProperties {
    
    // 网络属性配置文件
    private static Properties props = new Properties();
    
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                // 加载系统文件%JAVA_HOME%\conf\net.properties中设定的网络属性
                loadDefaultProperties();
                return null;
            }
        });
    }
    
    
    private NetProperties() {
    }
    
    /**
     * Get a networking system property. If no system property was defined
     * returns the default value, if it exists, otherwise returns
     * <code>null</code>.
     *
     * @param key the property name.
     *
     * @return the <code>String</code> value for the property,
     * or <code>null</code>
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkPropertiesAccess</code> method doesn't allow access
     *                           to the system properties.
     */
    // 从运行参数中获取指定的网络属性；如果未找到，则从net.properties配置中查找
    public static String get(String key) {
        String def = props.getProperty(key);
    
        try {
            return System.getProperty(key, def);
        } catch(IllegalArgumentException | NullPointerException e) {
            // ...
        }
    
        return null;
    }
    
    /**
     * Get a Boolean networking system property. If no system property was
     * defined returns the default value, if it exists, otherwise returns
     * <code>null</code>.
     *
     * @param key the property name.
     *
     * @return the <code>Boolean</code> value for the property,
     * or <code>null</code>
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkPropertiesAccess</code> method doesn't allow access
     *                           to the system properties.
     */
    // 从运行参数中获取布尔类型的网络属性；如果未找到，则从net.properties配置中查找
    public static Boolean getBoolean(String key) {
        String val = null;
        
        try {
            val = System.getProperty(key, props.getProperty(key));
        } catch(IllegalArgumentException | NullPointerException e) {
            // ...
        }
        
        if(val != null) {
            try {
                return Boolean.valueOf(val);
            } catch(NumberFormatException ex) {
                // ...
            }
        }
        
        return null;
    }
    
    /**
     * Get an Integer networking system property. If no system property was
     * defined returns the default value, if it exists, otherwise returns
     * <code>null</code>.
     *
     * @param key    the property name.
     * @param defval the default value to use if the property is not found
     *
     * @return the <code>Integer</code> value for the property,
     * or <code>null</code>
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkPropertiesAccess</code> method doesn't allow access
     *                           to the system properties.
     */
    // 从运行参数中获取整型类型的网络属性；如果未找到，则从net.properties配置中查找；如果还未找到，返回默认值
    public static Integer getInteger(String key, int defval) {
        String val = null;
        
        try {
            val = System.getProperty(key, props.getProperty(key));
        } catch(IllegalArgumentException | NullPointerException e) {
            // ...
        }
        
        if(val != null) {
            try {
                return Integer.decode(val);
            } catch(NumberFormatException ex) {
                // ...
            }
        }
        
        return defval;
    }
    
    /**
     * Loads the default networking system properties the file is in jre/lib/net.properties
     */
    // 加载系统文件%JAVA_HOME%\conf\net.properties中设定的网络属性
    private static void loadDefaultProperties() {
        // 获取JDK根目录
        String fname = StaticProperty.javaHome();
        if(fname == null) {
            throw new Error("Can't find java.home ??");
        }
        
        try {
            // 获取文件%JAVA_HOME%\conf\net.properties
            File f = new File(new File(fname, "conf"), "net.properties");
            fname = f.getCanonicalPath();
            
            // 创建指向net.properties文件的输入流
            BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fname));
            
            // 从properties文件加载属性集
            props.load(bin);
            
            // 关闭输入流
            bin.close();
        } catch(Exception e) {
            // Do nothing. We couldn't find or access the file
            // so we won't have default properties...
        }
    }
    
}
