/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.WatchEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides support for handling JDK-specific OpenOption, CopyOption and WatchEvent.Modifier types.
 */
// 对一部分文件操作的扩展可选参数以及目录监视修饰符提供统一支持
public final class ExtendedOptions {
    
    // 参见ExtendedWatchEventModifier：监视修饰符，表示向监视服务注册整个目录树，会监视子目录，而不是仅监视根目录
    public static final InternalOption<Void> FILE_TREE = new InternalOption<>();
    
    // 参见SensitivityWatchEventModifier：监视修饰符，指示监视服务的灵敏度
    public static final InternalOption<Integer> SENSITIVITY_HIGH = new InternalOption<>();
    public static final InternalOption<Integer> SENSITIVITY_MEDIUM = new InternalOption<>();
    public static final InternalOption<Integer> SENSITIVITY_LOW = new InternalOption<>();
    
    // 参见ExtendedOpenOption：扩展的文件创建/打开操作配置项
    public static final InternalOption<Void> NOSHARE_READ = new InternalOption<>();
    public static final InternalOption<Void> NOSHARE_WRITE = new InternalOption<>();
    public static final InternalOption<Void> NOSHARE_DELETE = new InternalOption<>();
    public static final InternalOption<Void> DIRECT = new InternalOption<>();
    
    // 参见ExtendedCopyOptionL：复制受线程中断影响
    public static final InternalOption<Void> INTERRUPTIBLE = new InternalOption<>();
    
    /** maps InternalOption to ExternalOption */
    private static final Map<InternalOption<?>, Wrapper<?>> internalToExternal = new ConcurrentHashMap<>();
    
    /**
     * The internal version of a JDK-specific OpenOption, CopyOption or WatchEvent.Modifier.
     */
    // 内部可选参数
    public static final class InternalOption<T> {
        
        InternalOption() {
        }
        
        private void registerInternal(Object option, T param) {
            Wrapper<T> wrapper = new Wrapper<T>(option, param);
            internalToExternal.put(this, wrapper);
        }
        
        /**
         * Register this internal option as a OpenOption.
         */
        public void register(OpenOption option) {
            registerInternal(option, null);
        }
        
        /**
         * Register this internal option as a CopyOption.
         */
        public void register(CopyOption option) {
            registerInternal(option, null);
        }
        
        /**
         * Register this internal option as a WatchEvent.Modifier.
         */
        public void register(WatchEvent.Modifier option) {
            registerInternal(option, null);
        }
        
        /**
         * Register this internal option as a WatchEvent.Modifier with the given parameter.
         */
        public void register(WatchEvent.Modifier option, T param) {
            registerInternal(option, param);
        }
        
        /**
         * Returns true if the given option (or modifier) maps to this internal option.
         */
        public boolean matches(Object option) {
            Wrapper<?> wrapper = internalToExternal.get(this);
    
            if(wrapper == null) {
                return false;
            } else {
                return option == wrapper.option;
            }
        }
        
        /**
         * Returns the parameter object associated with this internal option.
         */
        @SuppressWarnings("unchecked")
        public T parameter() {
            Wrapper<?> wrapper = internalToExternal.get(this);
            if(wrapper == null) {
                return null;
            } else {
                return (T) wrapper.parameter();
            }
        }
        
    }
    
    /**
     * Wraps an option or modifier.
     */
    // 对扩展可选参数或监视修饰符的包装
    private static final class Wrapper<T> {
        private final Object option;    // 扩展可选参数或监视修饰符
        private final T param;          // 附加参数
        
        Wrapper(Object option, T param) {
            this.option = option;
            this.param = param;
        }
        
        T parameter() {
            return param;
        }
    }
    
}
