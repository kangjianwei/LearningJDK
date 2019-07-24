/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A recursive result-bearing {@link ForkJoinTask}.
 *
 * <p>For a classic example, here is a task computing Fibonacci numbers:
 *
 * <pre> {@code
 * class Fibonacci extends RecursiveTask<Integer> {
 *   final int n;
 *   Fibonacci(int n) { this.n = n; }
 *   protected Integer compute() {
 *     if (n <= 1)
 *       return n;
 *     Fibonacci f1 = new Fibonacci(n - 1);
 *     f1.fork();
 *     Fibonacci f2 = new Fibonacci(n - 2);
 *     return f2.compute() + f1.join();
 *   }
 * }}</pre>
 *
 * However, besides being a dumb way to compute Fibonacci functions
 * (there is a simple fast linear algorithm that you'd use in
 * practice), this is likely to perform poorly because the smallest
 * subtasks are too small to be worthwhile splitting up. Instead, as
 * is the case for nearly all fork/join applications, you'd pick some
 * minimum granularity size (for example 10 here) for which you always
 * sequentially solve rather than subdividing.
 *
 * @author Doug Lea
 * @since 1.7
 */
// 带有返回值的任务
public abstract class RecursiveTask<V> extends ForkJoinTask<V> {
    private static final long serialVersionUID = 5232453952276485270L;
    
    /**
     * The result of the computation.
     */
    V result;   // 存储该任务的计算结果
    
    
    
    /*▼ 执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * The main computation performed by this task.
     *
     * @return the result of the computation
     */
    // 由子类完成执行任务的细节
    protected abstract V compute();
    
    /**
     * Implements execution conventions for RecursiveTask.
     */
    // 执行任务，并存储执行结果
    protected final boolean exec() {
        // 存储该任务执行后产生的值
        result = compute();
        return true;
    }
    
    /*▲ 执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回任务的执行结果
    public final V getRawResult() {
        return result;
    }
    
    // 设置任务的执行结果
    protected final void setRawResult(V value) {
        result = value;
    }
    
    /*▲ 任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
