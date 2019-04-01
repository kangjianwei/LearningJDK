/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * A task that can be scheduled for one-time or repeated execution by a
 * {@link Timer}.
 *
 * <p>A timer task is <em>not</em> reusable.  Once a task has been scheduled
 * for execution on a {@code Timer} or cancelled, subsequent attempts to
 * schedule it for execution will throw {@code IllegalStateException}.
 *
 * @author Josh Bloch
 * @since 1.3
 */
// 定时任务
public abstract class TimerTask implements Runnable {
    /**
     * This task has not yet been scheduled.
     */
    static final int VIRGIN    = 0; // 【初始化】
    /**
     * This task is scheduled for execution.  If it is a non-repeating task,
     * it has not yet been executed.
     */
    static final int SCHEDULED = 1; // 【排队】
    /**
     * This non-repeating task has already executed (or is currently
     * executing) and has not been cancelled.
     */
    static final int EXECUTED  = 2; // 【执行】
    /**
     * This task has been cancelled (with a call to TimerTask.cancel).
     */
    static final int CANCELLED = 3; // 【取消】
    
    /**
     * The state of this task, chosen from the constants below.
     */
    int state = VIRGIN; // 任务状态
    
    /**
     * Next execution time for this task in the format returned by
     * System.currentTimeMillis, assuming this task is scheduled for execution.
     * For repeating tasks, this field is updated prior to each task execution.
     */
    // 任务触发时间
    long nextExecutionTime;
    
    /**
     * Period in milliseconds for repeating tasks.  A positive value indicates
     * fixed-rate execution.  A negative value indicates fixed-delay execution.
     * A value of 0 indicates a non-repeating task.
     */
    /*
     * 任务的重复模式：
     *   零：非重复任务：只执行一次
     * 正数：重复性任务：固定周期，从任务初次被触发开始，以后每隔period时间就被触发一次
     * 负数：重复性任务：固定延时，任务下次的开始时间=任务上次结束时间+(-period)
     */
    long period = 0;
    
    /**
     * This object is used to control access to the TimerTask internals.
     */
    final Object lock = new Object();
    
    /**
     * Creates a new timer task.
     */
    protected TimerTask() {
    }
    
    /**
     * The action to be performed by this timer task.
     */
    // 执行任务
    public abstract void run();
    
    /**
     * Cancels this timer task.  If the task has been scheduled for one-time
     * execution and has not yet run, or has not yet been scheduled, it will
     * never run.  If the task has been scheduled for repeated execution, it
     * will never run again.  (If the task is running when this call occurs,
     * the task will run to completion, but will never run again.)
     *
     * <p>Note that calling this method from within the {@code run} method of
     * a repeating timer task absolutely guarantees that the timer task will
     * not run again.
     *
     * <p>This method may be called repeatedly; the second and subsequent
     * calls have no effect.
     *
     * @return true if this task is scheduled for one-time execution and has
     * not yet run, or this task is scheduled for repeated execution.
     * Returns false if the task was scheduled for one-time execution
     * and has already run, or if the task was never scheduled, or if
     * the task was already cancelled.  (Loosely speaking, this method
     * returns {@code true} if it prevents one or more scheduled
     * executions from taking place.)
     */
    // 取消处于【排队】状态的任务
    public boolean cancel() {
        synchronized(lock) {
            // 如果任务处于【排队】状态，则可以取消
            boolean result = (state == SCHEDULED);
            // 任务进入【取消】状态
            state = CANCELLED;
            return result;
        }
    }
    
    /**
     * Returns the <i>scheduled</i> execution time of the most recent
     * <i>actual</i> execution of this task.  (If this method is invoked
     * while task execution is in progress, the return value is the scheduled
     * execution time of the ongoing task execution.)
     *
     * <p>This method is typically invoked from within a task's run method, to
     * determine whether the current execution of the task is sufficiently
     * timely to warrant performing the scheduled activity:
     * <pre>{@code
     *   public void run() {
     *       if (System.currentTimeMillis() - scheduledExecutionTime() >=
     *           MAX_TARDINESS)
     *               return;  // Too late; skip this execution.
     *       // Perform the task
     *   }
     * }</pre>
     * This method is typically <i>not</i> used in conjunction with
     * <i>fixed-delay execution</i> repeating tasks, as their scheduled
     * execution times are allowed to drift over time, and so are not terribly
     * significant.
     *
     * @return the time at which the most recent execution of this task was
     * scheduled to occur, in the format returned by Date.getTime().
     * The return value is undefined if the task has yet to commence
     * its first execution.
     *
     * @see Date#getTime()
     */
    // 返回任务被安排去【排队】时的时间
    public long scheduledExecutionTime() {
        synchronized(lock) {
            return (period<0
                ? nextExecutionTime + period
                : nextExecutionTime - period);
        }
    }
}
