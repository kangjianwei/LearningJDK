/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
package java.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;

/**
 * This class holds a set of filenames to be deleted on VM exit through a shutdown hook.
 * A set is used both to prevent double-insertion of the same file as well as offer quick removal.
 */
// 文件删除钩子：注册一批在虚拟机关闭时会删掉的文件
class DeleteOnExitHook {
    
    // 保存待关闭文件
    private static LinkedHashSet<String> files = new LinkedHashSet<>();
    
    static {
        JavaLangAccess javaLangAccess = SharedSecrets.getJavaLangAccess();
        
        /*
         * DeleteOnExitHook must be the last shutdown hook to be invoked.
         * Application shutdown hooks may add the first file to the delete on exit list
         * and cause the DeleteOnExitHook to be registered during shutdown in progress.
         * So set the registerShutdownInProgress parameter to true.
         */
        // 将指定的钩子注册到2号插槽中，以便在虚拟机关闭时处理这些钩子
        javaLangAccess.registerShutdownHook(2                 /* Shutdown hook invocation order */, true              /* register even if shutdown in progress */, new Runnable() {
            public void run() {
                runHooks();
            }
        });
    }
    
    private DeleteOnExitHook() {
    }
    
    // 添加一个钩子：在虚拟机关闭时删除此文件
    static synchronized void add(String file) {
        if(files == null) {
            // DeleteOnExitHook is running. Too late to add a file
            throw new IllegalStateException("Shutdown in progress");
        }
        
        files.add(file);
    }
    
    // 执行文件删除钩子
    static void runHooks() {
        LinkedHashSet<String> theFiles;
        
        synchronized(DeleteOnExitHook.class) {
            theFiles = files;
            files = null;
        }
        
        ArrayList<String> toBeDeleted = new ArrayList<>(theFiles);
        
        /* reverse the list to maintain previous jdk deletion order. Last in first deleted. */
        // 逆转list中的元素，以便后进先出
        Collections.reverse(toBeDeleted);
        
        // 遍历所有注册文件，将其依次删掉
        for(String filename : toBeDeleted) {
            (new File(filename)).delete();
        }
    }
}
