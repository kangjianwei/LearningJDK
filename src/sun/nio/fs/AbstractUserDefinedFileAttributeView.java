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

import java.nio.ByteBuffer;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.io.IOException;
import java.util.*;

/**
 * Base implementation of UserDefinedAttributeView
 */
/*
 * "user"文件属性视图的抽象实现，这里的属性是接受用户定义的
 *
 * 注：不同的操作系统平台会有不同的实现机制
 * 在windows上，通过"备用数据流"来存取这些自定义属性；
 * 在linux上，通过扩展属性来设置/获取这些自定义属性；
 * 在mac上，目前未做实现。
 */
abstract class AbstractUserDefinedFileAttributeView implements UserDefinedFileAttributeView, DynamicFileAttributeView {
    
    protected AbstractUserDefinedFileAttributeView() {
    }
    
    // 返回当前属性视图的名称："user"
    @Override
    public final String name() {
        return "user";
    }
    
    /*
     * 向当前"user"文件属性视图中设置attName属性，设置的属性值为value；
     * 由于是自定义属性，所以属性名基本无限制，但属性值应当为byte[]或ByteBuffer类型的对象。
     */
    @Override
    public final void setAttribute(String attName, Object value) throws IOException {
        ByteBuffer src;
        
        if(value instanceof byte[]) {
            src = ByteBuffer.wrap((byte[]) value);
        } else {
            src = (ByteBuffer) value;
        }
        
        // 向当前"user"文件属性视图中写入一条名称为attName属性，写入的属性值为src。
        write(attName, src);
    }
    
    /**
     * 从当前"user"文件属性视图中获取一批属性的值；这批属性的名称存储在attNames中，获取到的属性以<属性名, 属性值>的形式返回；
     * 由于是自定义属性，所以属性名基本无限制，但属性值应当为byte[]或ByteBuffer类型的对象。
     */
    @Override
    public final Map<String, Object> readAttributes(String[] attNames) throws IOException {
        // names of attributes to return
        List<String> names = new ArrayList<>();
        
        // 确定待获取的属性值
        for(String name : attNames) {
            if(name.equals("*")) {
                names = list();
                break;
            } else {
                if(name.length() == 0) {
                    throw new IllegalArgumentException();
                }
                names.add(name);
            }
        }
        
        // read each value and return in map
        Map<String, Object> result = new HashMap<>();
        
        for(String name : names) {
            // 返回当前"user"文件属性视图下名为name的属性的值的尺寸
            int size = size(name);
            
            byte[] buf = new byte[size];
            
            // 包装一个字节数组为字节缓冲区
            ByteBuffer dst = ByteBuffer.wrap(buf);
            
            // 从当前"user"文件属性视图中读取一条名称为name属性，读取的属性值存入dst中
            int n = read(name, dst);
            
            byte[] value = (n == size) ? buf : Arrays.copyOf(buf, n);
            
            // 记录<属性名, 属性值>
            result.put(name, value);
        }
        
        return result;
    }
    
    protected void checkAccess(String file, boolean checkRead, boolean checkWrite) {
        assert checkRead || checkWrite;
        
        SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return;
        }
        
        if(checkRead) {
            sm.checkRead(file);
        }
        
        if(checkWrite) {
            sm.checkWrite(file);
        }
        
        sm.checkPermission(new RuntimePermission("accessUserDefinedAttributes"));
    }
    
}
