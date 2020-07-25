/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.util.Properties;
import sun.security.action.GetPropertyAction;

/**
 * This class defines a factory for creating DatagramSocketImpls. It defaults
 * to creating plain DatagramSocketImpls, but may create other DatagramSocketImpls
 * by setting the impl.prefix system property.
 *
 * For Windows versions lower than Windows Vista a TwoStacksPlainDatagramSocketImpl
 * is always created. This impl supports IPv6 on these platform where available.
 *
 * On Windows platforms greater than Vista that support a dual layer TCP/IP stack
 * a DualStackPlainDatagramSocketImpl is created for DatagramSockets. For MulticastSockets
 * a TwoStacksPlainDatagramSocketImpl is always created. This is to overcome the lack
 * of behavior defined for multicasting over a dual layer socket by the RFC.
 *
 * @author Chris Hegarty
 */
// 系统内置的"UDP-Socket委托"工厂，不同的平台上对该工厂有各自的实现
class DefaultDatagramSocketImplFactory {
    
    // 自定义的"UDP-Socket委托"实现类，以某个前缀起始
    private static final Class<?> prefixImplClass;
    
    /*
     * 是否偏向使用IP4地址
     *
     * 可以通过java.net.preferIPv4Stack属性设置此字段
     */
    private static final boolean preferIPv4Stack;
    
    /** True if exclusive binding is on for Windows */
    /*
     * windows平台上的选项：是否使用独占的绑定
     *
     * 可以通过sun.net.useExclusiveBind属性设置此字段
     */
    private static final boolean exclusiveBind;
    
    
    static {
        Class<?> prefixImplClassLocal = null;
        
        Properties props = GetPropertyAction.privilegedGetProperties();
        preferIPv4Stack = Boolean.parseBoolean(props.getProperty("java.net.preferIPv4Stack"));
        
        String exclBindProp = props.getProperty("sun.net.useExclusiveBind", "");
        exclusiveBind = (exclBindProp.isEmpty()) || Boolean.parseBoolean(exclBindProp);
        
        // impl.prefix
        String prefix = null;
        try {
            prefix = props.getProperty("impl.prefix");
            if(prefix != null) {
                prefixImplClassLocal = Class.forName("java.net." + prefix + "DatagramSocketImpl");
            }
        } catch(Exception e) {
            System.err.println("Can't find class: java.net." + prefix + "DatagramSocketImpl: check impl.prefix property");
        }
        
        prefixImplClass = prefixImplClassLocal;
    }
    
    
    /**
     * Creates a new <code>DatagramSocketImpl</code> instance.
     *
     * @param isMulticast true if this impl is to be used for a MutlicastSocket
     *
     * @return a new instance of <code>PlainDatagramSocketImpl</code>.
     */
    /*
     * 创建一个"UDP-Socket委托"，不同的平台上会有不同的实现。
     *
     * isMulticast: 是否为组播
     */
    static DatagramSocketImpl createDatagramSocketImpl(boolean isMulticast) throws SocketException {
        // 如果存在自定义的"UDP-Socket委托"实现类，则使用该实现类构造"UDP-Socket委托"实例
        if(prefixImplClass != null) {
            try {
                @SuppressWarnings("deprecation")
                Object result = prefixImplClass.newInstance();
                return (DatagramSocketImpl) result;
            } catch(Exception e) {
                throw new SocketException("can't instantiate DatagramSocketImpl");
            }
        }
        
        // 如果仅需支持IP4地址，或者使用了组播
        if(preferIPv4Stack || isMulticast) {
            // 兼容Vista以下的系统
            return new TwoStacksPlainDatagramSocketImpl(exclusiveBind && !isMulticast);
        } else {
            // 兼容Vista以上的系统
            return new DualStackPlainDatagramSocketImpl(exclusiveBind);
        }
    }
    
}
