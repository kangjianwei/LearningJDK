/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;

/**
 * Base implementation of AclFileAttributeView
 */
/*
 * "acl"文件属性视图的抽象实现，包含了关联文件的访问权限信息，可以代理"owner"文件属性视图(FileOwnerAttributeView)的功能
 *
 * 注：windows/linux/mac平台上均存在此抽象实现，但只有windows平台上会进一步提供对当前抽象类的完整实现
 */
abstract class AbstractAclFileAttributeView implements AclFileAttributeView, DynamicFileAttributeView {
    private static final String OWNER_NAME = "owner";   // "owner"属性
    private static final String ACL_NAME = "acl";     // "acl"属性
    
    // 返回当前属性视图的名称："acl"
    @Override
    public final String name() {
        return "acl";
    }
    
    /*
     * 向当前"acl"文件属性视图中设置attName属性，设置的属性值为value，可设置的属性包括：
     * - owner: "owner"属性，其值是UserPrincipal类型的对象
     * - acl  : "acl"属性，其值是List<AclEntry>类型的对象
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void setAttribute(String attName, Object value) throws IOException {
        
        // 需要设置"owner"属性
        if(attName.equals(OWNER_NAME)) {
            setOwner((UserPrincipal) value);
            return;
        }
        
        // 需要设置"acl"属性
        if(attName.equals(ACL_NAME)) {
            setAcl((List<AclEntry>) value);
            return;
        }
        
        throw new IllegalArgumentException("'" + name() + ":" + attName + "' not recognized");
    }
    
    /*
     * 从当前"acl"文件属性视图中获取一批属性的值；这批属性的名称存储在attNames中，获取到的属性以<属性名, 属性值>的形式返回，可获取的属性包括：
     * - owner: "owner"属性，其值是UserPrincipal类型的对象
     * - acl  : "acl"属性，其值是List<AclEntry>类型的对象
     */
    @Override
    public final Map<String, Object> readAttributes(String[] attNames) throws IOException {
        boolean requestAcl = false;   // 需要获取"acl"属性
        boolean requestOwner = false;   // 需要获取"owner"属性
        
        for(String attribute : attNames) {
            if(attribute.equals("*")) {
                requestOwner = true;
                requestAcl = true;
                continue;
            }
            
            if(attribute.equals(ACL_NAME)) {
                requestAcl = true;
                continue;
            }
            
            if(attribute.equals(OWNER_NAME)) {
                requestOwner = true;
                continue;
            }
            
            throw new IllegalArgumentException("'" + name() + ":" + attribute + "' not recognized");
        }
        
        Map<String, Object> result = new HashMap<>(2);
        
        if(requestAcl) {
            // 返回关联文件的访问控制列表(ACL)
            List<AclEntry> acl = getAcl();
            
            result.put(ACL_NAME, acl);
        }
        
        if(requestOwner) {
            // 返回关联文件的所有者
            UserPrincipal owner = getOwner();
            
            result.put(OWNER_NAME, owner);
        }
        
        return Collections.unmodifiableMap(result);
    }
    
}
