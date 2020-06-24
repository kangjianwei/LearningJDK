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
 * An implementation of FileOwnerAttributeView that delegates to a given
 * PosixFileAttributeView or AclFileAttributeView object.
 */
/*
 * "owner"文件属性视图的实现，可以获取/设置文件所有者信息
 *
 * 注：不同的操作系统平台会有不同的实现机制
 * 在windows上，该实现是委托给AclFileAttributeView来完成的；
 * 在linux和mac上，该实现是委托给PosixFileAttributeView来完成的。
 */
final class FileOwnerAttributeViewImpl implements FileOwnerAttributeView, DynamicFileAttributeView {
    private static final String OWNER_NAME = "owner";   // "owner"属性
    
    // 当前类的一个代理，在windows上是AclFileAttributeView，在linux和mac上是PosixFileAttributeView
    private final FileAttributeView view;
    
    // 判断view是否为PosixFileAttributeView
    private final boolean isPosixView;
    
    // windows上通过此途径构造FileOwnerAttributeView
    FileOwnerAttributeViewImpl(AclFileAttributeView view) {
        this.view = view;
        this.isPosixView = false;
    }
    
    // linux和mac上通过此途径构造FileOwnerAttributeView
    FileOwnerAttributeViewImpl(PosixFileAttributeView view) {
        this.view = view;
        this.isPosixView = true;
    }
    
    // 返回当前属性视图的名称："owner"
    @Override
    public String name() {
        return "owner";
    }
    
    /*
     * 向当前"owner"文件属性视图中设置attName属性，设置的属性值为value，可设置的属性包括：
     * - owner: "owner"属性，其值是UserPrincipal类型的对象
     */
    @Override
    public void setAttribute(String attName, Object value) throws IOException {
        if(attName.equals(OWNER_NAME)) {
            setOwner((UserPrincipal) value);
        } else {
            throw new IllegalArgumentException("'" + name() + ":" + attName + "' not recognized");
        }
    }
    
    /**
     * 从当前"owner"文件属性视图中获取一批属性的值；这批属性的名称由attNames给出，获取到的属性以<属性名, 属性值>的形式返回，可获取的属性包括：
     * - owner: "owner"属性，其值是UserPrincipal类型的对象
     */
    @Override
    public Map<String, Object> readAttributes(String[] attributes) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        for(String attribute : attributes) {
            if(attribute.equals("*") || attribute.equals(OWNER_NAME)) {
                result.put(OWNER_NAME, getOwner());
            } else {
                throw new IllegalArgumentException("'" + name() + ":" + attribute + "' not recognized");
            }
        }
        return result;
    }
    
    // 返回关联文件的所有者
    @Override
    public UserPrincipal getOwner() throws IOException {
        if(isPosixView) {
            return ((PosixFileAttributeView) view).readAttributes().owner();
        } else {
            return ((AclFileAttributeView) view).getOwner();
        }
    }
    
    // 更新/设置关联文件的所有者信息
    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        if(isPosixView) {
            ((PosixFileAttributeView) view).setOwner(owner);
        } else {
            ((AclFileAttributeView) view).setOwner(owner);
        }
    }
}
