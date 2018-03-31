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

package jdk.internal.ref;

import jdk.internal.misc.InnocuousThread;

import java.lang.ref.Cleaner;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;

/**
 * CleanerFactory provides a Cleaner for use within system modules.
 * The cleaner is created on the first reference to the CleanerFactory.
 */
// 清理器工厂，返回一个公用的特殊清理器(Cleaner)，用于系统模块
public final class CleanerFactory {
    /* The common Cleaner. */
    private final static Cleaner commonCleaner = Cleaner.create(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Thread run() {
                    Thread t = InnocuousThread.newSystemThread("Common-Cleaner", r);
                    t.setPriority(Thread.MAX_PRIORITY - 2);
                    return t;
                }
            });
        }
    });
    
    /**
     * Cleaner for use within system modules.
     *
     * This Cleaner will run on a thread whose context class loader is {@code null}.
     * The system cleaning action to perform in this Cleaner should handle a {@code null} context class loader.
     *
     * @return a Cleaner for use within system modules
     */
    /*
     * 该清理器运行在一个没有类加载器（被设置为null）的线程中。
     * 因为执行某些系统清理操作需要一个空的类加载器上下文。
     */
    public static Cleaner cleaner() {
        return commonCleaner;
    }
}
