/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Windows implementation of AclFileAttributeView.
 */
// windows平台上实现的"acl"文件属性视图
class WindowsAclFileAttributeView extends AbstractAclFileAttributeView {
    
    /**
     * typedef struct _SECURITY_DESCRIPTOR {
     * BYTE  Revision;
     * BYTE  Sbz1;
     * SECURITY_DESCRIPTOR_CONTROL Control;
     * PSID Owner;
     * PSID Group;
     * PACL Sacl;
     * PACL Dacl;
     * } SECURITY_DESCRIPTOR;
     */
    private static final short SIZEOF_SECURITY_DESCRIPTOR = 20;
    
    private final WindowsPath file;     // 当前视图关联的文件
    private final boolean followLinks;  // 对于符号链接，是否将其链接到目标文件
    
    WindowsAclFileAttributeView(WindowsPath file, boolean followLinks) {
        this.file = file;
        this.followLinks = followLinks;
    }
    
    // 返回关联文件的所有者
    @Override
    public UserPrincipal getOwner() throws IOException {
        checkAccess(file, true, false);
        
        // GetFileSecurity does not follow links so when following links we need the final target
        String path = WindowsLinkSupport.getFinalPath(file, followLinks);
        
        // 返回从指定文件中读取到的包含"所有者"信息的安全描述符
        NativeBuffer buffer = getFileSecurity(path, OWNER_SECURITY_INFORMATION);
        
        try {
            /* get the address of the SID */
            // 从指定的安全描述符中读取"所有者"信息
            long sidAddress = GetSecurityDescriptorOwner(buffer.address());
            if(sidAddress == 0L) {
                throw new IOException("no owner");
            }
            
            // 将本地获取到的"所有者"信息转换为Java层的对象
            return WindowsUserPrincipals.fromSid(sidAddress);
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
            return null;
        } finally {
            buffer.release();
        }
    }
    
    // 更新/设置关联文件的所有者信息
    @Override
    public void setOwner(UserPrincipal obj) throws IOException {
        if(obj == null) {
            throw new NullPointerException("'owner' is null");
        }
        
        if(!(obj instanceof WindowsUserPrincipals.User)) {
            throw new ProviderMismatchException();
        }
        
        WindowsUserPrincipals.User owner = (WindowsUserPrincipals.User) obj;
        
        // permission check
        checkAccess(file, false, true);
        
        // SetFileSecurity does not follow links so when following links we need the final target
        String path = WindowsLinkSupport.getFinalPath(file, followLinks);
        
        // ConvertStringSidToSid allocates memory for SID so must invoke LocalFree to free it when we are done
        long pOwner = 0L;
        try {
            pOwner = ConvertStringSidToSid(owner.sidString());
        } catch(WindowsException x) {
            throw new IOException("Failed to get SID for " + owner.getName() + ": " + x.errorString());
        }
        
        // Allocate buffer for security descriptor, initialize it, set owner information and update the file.
        try {
            NativeBuffer buffer = NativeBuffers.getNativeBuffer(SIZEOF_SECURITY_DESCRIPTOR);
            try {
                InitializeSecurityDescriptor(buffer.address());
                SetSecurityDescriptorOwner(buffer.address(), pOwner);
                
                // may need SeRestorePrivilege to set the owner
                WindowsSecurity.Privilege priv = WindowsSecurity.enablePrivilege("SeRestorePrivilege");
                try {
                    SetFileSecurity(path, OWNER_SECURITY_INFORMATION, buffer.address());
                } finally {
                    priv.drop();
                }
            } catch(WindowsException x) {
                x.rethrowAsIOException(file);
            } finally {
                buffer.release();
            }
        } finally {
            LocalFree(pOwner);
        }
    }
    
    // 返回关联文件的访问控制列表(ACL)
    @Override
    public List<AclEntry> getAcl() throws IOException {
        checkAccess(file, true, false);
        
        /* GetFileSecurity does not follow links so when following links we need the final target */
        // 获取file处文件的最终路径
        String path = WindowsLinkSupport.getFinalPath(file, followLinks);
        
        /*
         * ALLOW and DENY entries in DACL;
         * AUDIT entries in SACL (ignore for now as it requires privileges)
         */
        // 自主访问控制列表，指示user和group的访问权限
        NativeBuffer buffer = getFileSecurity(path, DACL_SECURITY_INFORMATION);
        try {
            return WindowsSecurityDescriptor.getAcl(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    // 为关联文件设置访问控制列表(ACL)
    @Override
    public void setAcl(List<AclEntry> acl) throws IOException {
        checkAccess(file, false, true);
        
        /* SetFileSecurity does not follow links so when following links we need the final target */
        // 获取file处文件的最终路径
        String path = WindowsLinkSupport.getFinalPath(file, followLinks);
        
        // 返回创建的windows安全描述符，其中的ACL信息由参数acl给出
        WindowsSecurityDescriptor sd = WindowsSecurityDescriptor.create(acl);
        
        try {
            // 为指定的文件设置安全描述符
            SetFileSecurity(path, DACL_SECURITY_INFORMATION, sd.address());
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
        } finally {
            sd.release();
        }
    }
    
    /** invokes GetFileSecurity to get requested security information */
    // 返回从指定文件中读取到的安全属性，request是请求读取的安全信息
    static NativeBuffer getFileSecurity(String path, int request) throws IOException {
        // invoke get to buffer size
        int size = 0;
        
        try {
            // (试探)读取指定文件的安全属性，从中可以获取到文件的所有者和用户组信息；如果读取成功，返回读到的信息长度
            size = GetFileSecurity(path, request, 0L, 0);
        } catch(WindowsException x) {
            x.rethrowAsIOException(path);
        }
        
        assert size>0;
        
        // allocate buffer and re-invoke to get security information
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);
        try {
            for(; ; ) {
                // ()读取指定文件的安全属性，从中可以获取到文件的所有者和用户组信息；如果读取成功，返回读到的信息长度
                int newSize = GetFileSecurity(path, request, buffer.address(), size);
                if(newSize<=size) {
                    return buffer;
                }
                
                // buffer was insufficient
                buffer.release();
                buffer = NativeBuffers.getNativeBuffer(newSize);
                size = newSize;
            }
        } catch(WindowsException x) {
            buffer.release();
            x.rethrowAsIOException(path);
            return null;
        }
    }
    
    // permission check
    private void checkAccess(WindowsPath file, boolean checkRead, boolean checkWrite) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            if(checkRead) {
                sm.checkRead(file.getPathForPermissionCheck());
            }
            if(checkWrite) {
                sm.checkWrite(file.getPathForPermissionCheck());
            }
            sm.checkPermission(new RuntimePermission("accessUserInformation"));
        }
    }
    
}
