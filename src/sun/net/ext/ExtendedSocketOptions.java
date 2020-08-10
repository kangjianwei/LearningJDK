/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.ext;

import java.io.FileDescriptor;
import java.net.SocketException;
import java.net.SocketOption;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the infrastructure to support extended socket options,
 * beyond those defined in {@link java.net.StandardSocketOptions}.
 *
 * Extended socket options are accessed through the jdk.net API, which is in
 * the jdk.net module.
 */
// 扩展的Socket参数
public abstract class ExtendedSocketOptions {
    
    public static final short SOCK_STREAM = 1;
    public static final short SOCK_DGRAM = 2;
    
    private static volatile ExtendedSocketOptions instance;
    
    private final Set<SocketOption<?>> options;
    
    static {
        try {
            /* If the class is present, it will be initialized which triggers registration of the extended socket options. */
            // 加载jdk.net.ExtendedSocketOptions类
            Class<?> c = Class.forName("jdk.net.ExtendedSocketOptions");
        } catch(ClassNotFoundException e) {
            // the jdk.net module is not present => no extended socket options
            instance = new NoExtendedSocketOptions();
        }
    }
    
    protected ExtendedSocketOptions(Set<SocketOption<?>> options) {
        this.options = options;
    }
    
    // 返回ExtendedSocketOptions实例
    public static final ExtendedSocketOptions getInstance() {
        return instance;
    }
    
    /** Registers support for extended socket options. Invoked by the jdk.net module. */
    // 设置ExtendedSocketOptions实例
    public static final void register(ExtendedSocketOptions extOptions) {
        if(instance != null) {
            throw new InternalError("Attempting to reregister extended options");
        }
        
        instance = extOptions;
    }
    
    /** Returns the value of a socket option, for the given socket. */
    // 返回socket指定参数的值
    public abstract Object getOption(FileDescriptor socket, SocketOption<?> option) throws SocketException;
    
    /** Sets the value of a socket option, for the given socket. */
    // 为socket的指定参数设置值
    public abstract void setOption(FileDescriptor socket, SocketOption<?> option, Object value) throws SocketException;
    
    /** Tells whether or not the option is supported. */
    // 判断当前平台是否支持指定的Socket扩展参数
    public final boolean isOptionSupported(SocketOption<?> option) {
        return options().contains(option);
    }
    
    /** Return the, possibly empty, set of extended socket options available. */
    // 返回Socket扩展参数集
    public final Set<SocketOption<?>> options() {
        return options;
    }
    
    // 返回指定类型的Socket扩展参数集
    public static final Set<SocketOption<?>> options(short type) {
        return getInstance().options0(type);
    }
    
    private Set<SocketOption<?>> options0(short type) {
        Set<SocketOption<?>> extOptions = null;
        switch(type) {
            case SOCK_DGRAM:
                extOptions = options.stream().filter((option) -> !option.name().startsWith("TCP_")).collect(Collectors.toUnmodifiableSet());
                break;
            case SOCK_STREAM:
                extOptions = options.stream().filter((option) -> !option.name().startsWith("UDP_")).collect(Collectors.toUnmodifiableSet());
                break;
            default:
                //this will never happen
                throw new IllegalArgumentException("Invalid socket option type");
        }
        return extOptions;
    }
    
    // 空参数集
    static final class NoExtendedSocketOptions extends ExtendedSocketOptions {
        
        NoExtendedSocketOptions() {
            super(Collections.emptySet());
        }
        
        @Override
        public void setOption(FileDescriptor fd, SocketOption<?> option, Object value) throws SocketException {
            throw new UnsupportedOperationException("no extended options: " + option.name());
        }
        
        @Override
        public Object getOption(FileDescriptor fd, SocketOption<?> option) throws SocketException {
            throw new UnsupportedOperationException("no extended options: " + option.name());
        }
    }
    
}
