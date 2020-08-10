/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;

// IP地址的缓存策略
public final class InetAddressCachePolicy {
    
    public static final int FOREVER = -1;   // 一直缓存
    public static final int NEVER = 0;      // 不缓存
    
    /**
     * The Java-level namelookup cache policy for successful lookups:
     *
     * -1:                 caching forever
     * any positive value: the number of seconds to cache an address for
     *
     * default value is forever (FOREVER), as we let the platform do the caching.
     * For security reasons, this caching is made forever when a security manager is set.
     */
    /*
     * 命中缓存策略
     *
     * 初值为FOREVER，即一直保存
     * 如果是非零整数，则表示需要缓存对应秒数的时长
     */
    private static volatile int cachePolicy = FOREVER;
    
    /**
     * The Java-level namelookup cache policy for negative lookups:
     *
     * -1:                 caching forever
     * any positive value: the number of seconds to cache an address for
     *
     * default value is 0.
     * It can be set to some other value for performance reasons.
     */
    /*
     * 未命中缓存策略
     *
     * 初值为NEVER，即不直保存
     * 如果是非零整数，则表示需要缓存对应秒数的时长
     */
    private static volatile int negativeCachePolicy = NEVER;
    
    /** default value for positive lookups */
    // 默认的命中缓存策略：保存30秒
    public static final int DEFAULT_POSITIVE = 30;
    
    // Controls the cache policy for successful lookups only
    private static final String cachePolicyProp = "networkaddress.cache.ttl";       // 系统属性，设置命中缓存策略
    private static final String cachePolicyPropFallback = "sun.net.inetaddr.ttl";   // 系统属性，设置命中缓存策略
    
    // Controls the cache policy for negative lookups only
    private static final String negativeCachePolicyProp = "networkaddress.cache.negative.ttl";      // 系统属性，设置未命中缓存策略
    private static final String negativeCachePolicyPropFallback = "sun.net.inetaddr.negative.ttl";  // 系统属性，设置未命中缓存策略
    
    /**
     * Whether or not the cache policy for successful lookups was set
     * using a property (cmd line).
     */
    private static boolean propertySet;         // 是否在系统属性中设置了命中缓存策略
    
    /**
     * Whether or not the cache policy for negative lookups was set
     * using a property (cmd line).
     */
    private static boolean propertyNegativeSet; // 是否在系统属性中设置了未命中缓存策略
    
    /* 初始化缓存策略参数 */
    static {
        
        Integer tmp = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            public Integer run() {
                try {
                    String tmpString = Security.getProperty(cachePolicyProp);
                    if(tmpString != null) {
                        return Integer.valueOf(tmpString);
                    }
                } catch(NumberFormatException ignored) {
                    // Ignore
                }
                
                try {
                    String tmpString = System.getProperty(cachePolicyPropFallback);
                    if(tmpString != null) {
                        return Integer.decode(tmpString);
                    }
                } catch(NumberFormatException ignored) {
                    // Ignore
                }
                return null;
            }
        });
        
        if(tmp != null) {
            cachePolicy = tmp<0 ? FOREVER : tmp;
            propertySet = true;
        } else {
            /*
             * No properties defined for positive caching.
             * If there is no security manager then use the default positive cache value.
             */
            if(System.getSecurityManager() == null) {
                cachePolicy = DEFAULT_POSITIVE;
            }
        }
        
        tmp = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            public Integer run() {
                try {
                    String tmpString = Security.getProperty(negativeCachePolicyProp);
                    if(tmpString != null) {
                        return Integer.valueOf(tmpString);
                    }
                } catch(NumberFormatException ignored) {
                    // Ignore
                }
                
                try {
                    String tmpString = System.getProperty(negativeCachePolicyPropFallback);
                    if(tmpString != null) {
                        return Integer.decode(tmpString);
                    }
                } catch(NumberFormatException ignored) {
                    // Ignore
                }
                return null;
            }
        });
        
        if(tmp != null) {
            negativeCachePolicy = tmp<0 ? FOREVER : tmp;
            propertyNegativeSet = true;
        }
    }
    
    // 返回命中缓存策略
    public static int get() {
        return cachePolicy;
    }
    
    // 返回未命中缓存策略
    public static int getNegative() {
        return negativeCachePolicy;
    }
    
    /**
     * Sets the cache policy for successful lookups if the user has not
     * already specified a cache policy for it using a
     * command-property.
     *
     * @param newPolicy the value in seconds for how long the lookup
     *                  should be cached
     */
    // 设置命中缓存策略
    public static synchronized void setIfNotSet(int newPolicy) {
        /*
         * When setting the new value we may want to signal that the
         * cache should be flushed, though this doesn't seem strictly
         * necessary.
         */
        if(!propertySet) {
            checkValue(newPolicy, cachePolicy);
            cachePolicy = newPolicy;
        }
    }
    
    /**
     * Sets the cache policy for negative lookups if the user has not
     * already specified a cache policy for it using a
     * command-property.
     *
     * @param newPolicy the value in seconds for how long the lookup
     *                  should be cached
     */
    // 设置未命中缓存策略
    public static void setNegativeIfNotSet(int newPolicy) {
        /*
         * When setting the new value we may want to signal that the
         * cache should be flushed, though this doesn't seem strictly
         * necessary.
         */
        if(!propertyNegativeSet) {
            // Negative caching does not seem to have any security implications.
            // checkValue(newPolicy, negativeCachePolicy); but we should normalize negative policy
            negativeCachePolicy = newPolicy<0 ? FOREVER : newPolicy;
        }
    }
    
    // 检查缓存策略参数：新的策略应相比旧的策略，缓存时间应当至少一样长
    private static void checkValue(int newPolicy, int oldPolicy) {
        /*
         * If malicious code gets a hold of this method, prevent
         * setting the cache policy to something laxer or some
         * invalid negative value.
         */
        if(newPolicy == FOREVER) {
            return;
        }
        
        if((oldPolicy == FOREVER) || (newPolicy<oldPolicy) || (newPolicy<FOREVER)) {
            throw new SecurityException("can't make InetAddress cache more lax");
        }
    }
    
}
