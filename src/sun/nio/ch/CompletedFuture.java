/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A Future representing the result of an I/O operation that has already completed.
 */
/*
 * "已完成的结果"，用来包装异步IO的操作结果。
 *
 * 如果某个异步IO操作立刻就可以知道执行结果，那么会生成一个CompletedFuture对象来接收那些执行结果。
 * 当前类通常用在那些IO操作没有成效的场合，比如遇到了异常，或者遇到了EOF。
 *
 * 参见：PendingFuture。
 */
final class CompletedFuture<V> implements Future<V> {
    
    private final V result;         // 任务执行的返回值
    private final Throwable exc;    // 任务执行中抛出的异常
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private CompletedFuture(V result, Throwable exc) {
        this.result = result;
        this.exc = exc;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取IO操作的执行结果(该结果可以是异常)；如果任务未完成，则陷入阻塞以等待任务完成
    @Override
    public V get() throws ExecutionException {
        if(exc != null) {
            throw new ExecutionException(exc);
        }
        
        return result;
    }
    
    // 获取IO操作的执行结果(该结果可以是异常)；如果任务未完成，则阻塞一段时间以等待任务完成；如果超时后还没等到任务完成，会抛出超时异常
    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        if(exc != null) {
            throw new ExecutionException(exc);
        }
        
        return result;
    }
    
    /*▲ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 包装任务的计算结果
    static <V> CompletedFuture<V> withResult(V result) {
        return new CompletedFuture<V>(result, null);
    }
    
    // 包装任务抛出的异常
    static <V> CompletedFuture<V> withFailure(Throwable exc) {
        // exception must be IOException or SecurityException
        if(!(exc instanceof IOException) && !(exc instanceof SecurityException)) {
            exc = new IOException(exc);
        }
        
        return new CompletedFuture<V>(null, exc);
    }
    
    // 包装任务的计算结果或包装任务抛出的异常（优先处理异常）
    static <V> CompletedFuture<V> withResult(V result, Throwable exc) {
        if(exc == null) {
            return withResult(result);
        } else {
            return withFailure(exc);
        }
    }
    
    /*▲ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中止 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 中止异步任务，包括取消或中断，参数表示是否可在任务执行期间中断线程（此处总是返回false，代表无法中止）
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
    
    /*▲ 中止 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 是否已向当前对象填充了任务执行结果(异步IO操作是否已完成)
    @Override
    public boolean isDone() {
        return true;
    }
    
    // 判断当前任务是否已中止
    @Override
    public boolean isCancelled() {
        return false;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
