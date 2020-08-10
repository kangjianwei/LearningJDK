/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http;

import java.net.Authenticator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class used to tie a key to an authenticator instance.
 */
// 工具类，用来获取身份认证器的key
public final class AuthenticatorKeys {
    
    public static final String DEFAULT = "default";
    
    private static final AtomicLong IDS = new AtomicLong();
    
    // 用来访问身份认证器的key的工具
    private static AuthenticatorKeyAccess authenticatorKeyAccess;
    
    private AuthenticatorKeys() {
        throw new InternalError("Trying to instantiate static class");
    }
    
    // 返回指定身份认证器的key
    public static String computeKey(Authenticator authenticator) {
        return System.identityHashCode(authenticator) + "-" + IDS.incrementAndGet() + "@" + authenticator.getClass().getName();
    }
    
    /**
     * Returns a key for the given authenticator.
     *
     * @param authenticator The authenticator; {@code null} should be
     *                      passed when the {@linkplain
     *                      Authenticator#setDefault(java.net.Authenticator) default}
     *                      authenticator is meant.
     *
     * @return A key for the given authenticator, {@link #DEFAULT} for {@code null}.
     */
    // 返回指定身份认证器的key
    public static String getKey(Authenticator authenticator) {
        if(authenticator == null) {
            return DEFAULT;
        }
        
        return authenticatorKeyAccess.getKey(authenticator);
    }
    
    // 设置一个AuthenticatorKeyAccess，以便后续获取指定身份认证器的key
    public static void setAuthenticatorKeyAccess(AuthenticatorKeyAccess access) {
        if(authenticatorKeyAccess == null && access != null) {
            authenticatorKeyAccess = access;
        }
    }
    
    
    // 访问身份认证器的key的接口
    @FunctionalInterface
    public interface AuthenticatorKeyAccess {
        String getKey(Authenticator authenticator);
    }
    
}
