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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A synchronization aid that allows a set of threads to all wait for
 * each other to reach a common barrier point.  CyclicBarriers are
 * useful in programs involving a fixed sized party of threads that
 * must occasionally wait for each other. The barrier is called
 * <em>cyclic</em> because it can be re-used after the waiting threads
 * are released.
 *
 * <p>A {@code CyclicBarrier} supports an optional {@link Runnable} command
 * that is run once per barrier point, after the last thread in the party
 * arrives, but before any threads are released.
 * This <em>barrier action</em> is useful
 * for updating shared-state before any of the parties continue.
 *
 * <p><b>Sample usage:</b> Here is an example of using a barrier in a
 * parallel decomposition design:
 *
 * <pre> {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction = () -> mergeRows(...);
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}</pre>
 *
 * Here, each worker thread processes a row of the matrix then waits at the
 * barrier until all rows have been processed. When all rows are processed
 * the supplied {@link Runnable} barrier action is executed and merges the
 * rows. If the merger
 * determines that a solution has been found then {@code done()} will return
 * {@code true} and each worker will terminate.
 *
 * <p>If the barrier action does not rely on the parties being suspended when
 * it is executed, then any of the threads in the party could execute that
 * action when it is released. To facilitate this, each invocation of
 * {@link #await} returns the arrival index of that thread at the barrier.
 * You can then choose which thread should execute the barrier action, for
 * example:
 * <pre> {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}</pre>
 *
 * <p>The {@code CyclicBarrier} uses an all-or-none breakage model
 * for failed synchronization attempts: If a thread leaves a barrier
 * point prematurely because of interruption, failure, or timeout, all
 * other threads waiting at that barrier point will also leave
 * abnormally via {@link BrokenBarrierException} (or
 * {@link InterruptedException} if they too were interrupted at about
 * the same time).
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * {@code await()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions that are part of the barrier action, which in turn
 * <i>happen-before</i> actions following a successful return from the
 * corresponding {@code await()} in other threads.
 *
 * @author Doug Lea
 * @see CountDownLatch
 * @since 1.5
 */
// 循环栅栏，凑够一组线程后批量唤醒，下一组线程达到时可以重用此工具
public class CyclicBarrier {
    /** The lock for guarding barrier entry */
    // 线程争用的独占锁，只有上一个线程陷入阻塞，下一个线程才能争锁
    private final ReentrantLock lock = new ReentrantLock();
    
    /** Condition to wait on until tripped */
    // 使线程陷入阻塞或唤醒线程
    private final Condition trip = lock.newCondition();
    
    /** The command to run when tripped */
    /*
     * 栅栏附带的动作，与线程自身的Runnable不同
     * barrierCommand在栅栏开启的那一刻执行
     * 换句话说，等所有线程都就位后，才执行barrierCommand
     * barrierCommand可用于在所有线程启动前，或所有线程执行完毕后，
     * 进行一些统一处理
     */
    private final Runnable barrierCommand;
    
    /** The current generation */
    // 栅栏，同一组的线程依赖同一个栅栏，不同组的线程使用不同的栅栏
    private Generation generation = new Generation();
    
    /** The number of parties */
    // 表示该栅栏内可以容纳的最大线程数量
    private final int parties;
    
    /**
     * Number of parties still waiting. Counts down from parties to 0
     * on each generation.  It is reset to parties on each new
     * generation or when broken.
     */
    // 倒数计数，初始值等于parties，直到减为0时，表示可以唤醒全部线程了
    private int count;
    
    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and which
     * will execute the given barrier action when the barrier is tripped,
     * performed by the last thread entering the barrier.
     *
     * @param parties       the number of threads that must invoke {@link #await}
     *                      before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *                      tripped, or {@code null} if there is no action
     *
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    // 初始化一个栅栏，parties表示可以容纳的最大线程数量，barrierAction是栅栏附带的动作
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if(parties<=0) {
            throw new IllegalArgumentException();
        }
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }
    
    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and
     * does not perform a predefined action when the barrier is tripped.
     *
     * @param parties the number of threads that must invoke {@link #await}
     *                before the barrier is tripped
     *
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    // 初始化一个栅栏，parties表示可以容纳的最大线程数量，栅栏自身无附带的动作
    public CyclicBarrier(int parties) {
        this(parties, null);
    }
    
    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while waiting,
     * then all other waiting threads will throw
     * {@link BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @return the arrival index of the current thread, where index
     * {@code getParties() - 1} indicates the first
     * to arrive and zero indicates the last to arrive
     *
     * @throws InterruptedException   if the current thread was interrupted
     *                                while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *                                interrupted or timed out while the current thread was
     *                                waiting, or the barrier was reset, or the barrier was
     *                                broken when {@code await} was called, or the barrier
     *                                action (if present) failed due to an exception
     */
    // 使线程陷入阻塞，直到凑够parties个线程后再被唤醒
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch(TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }
    
    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit    the time unit of the timeout parameter
     *
     * @return the arrival index of the current thread, where index
     * {@code getParties() - 1} indicates the first
     * to arrive and zero indicates the last to arrive
     *
     * @throws InterruptedException   if the current thread was interrupted
     *                                while waiting
     * @throws TimeoutException       if the specified timeout elapses.
     *                                In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *                                interrupted or timed out while the current thread was
     *                                waiting, or the barrier was reset, or the barrier was broken
     *                                when {@code await} was called, or the barrier action (if
     *                                present) failed due to an exception
     */
    // 使线程陷入阻塞，带有超时设置，如果在阻塞中没被其他线程唤醒，那么就认为该线程失效了，进而放弃整组线程
    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }
    
    /**
     * Main barrier code, covering the various policies.
     */
    /*
     * timed标记是否使用超时设置，nanos是设置的超时时间
     * 当timed==fasle，那么陷入阻塞的线程会一直等待被唤醒
     * 如果别的线程主动破坏了栅栏，那么整组线程将失效
     * 当timed==true，那么线程会阻塞一段时间
     * 如果线程在阻塞期间没被唤醒，而是自然醒的，
     * 那么就认为该线程超时了，那么整组线程都将失效
     */
    private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException, TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;
            
            if(g.broken) {
                throw new BrokenBarrierException();
            }
            
            if(Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
            
            int index = --count;
            
            //
            if(index == 0) {
                boolean ranAction = false;
                try {
                    // 执行栅栏附带的动作
                    final Runnable command = barrierCommand;
                    if(command != null) {
                        command.run();
                    }
                    
                    ranAction = true;
                    
                    // 唤醒其他parties-1个线程，生成新的栅栏，并重置计数器count
                    nextGeneration();
                    
                    return 0;
                } finally {
                    // 如果发生异常行为，需要破坏栅栏，放弃对该组线程的处理
                    if(!ranAction) {
                        breakBarrier();
                    }
                }
            }
            
            // loop until tripped, broken, interrupted, or timed out
            for(; ; ) {
                try {
                    if(!timed) {
                        // 如果没有设置超时标记，则一直阻塞下去，直到被别的线程唤醒
                        trip.await();
                    } else {
                        if(nanos>0L) {
                            // 如果设置了超时标记，尝试休眠nanos纳秒，返回残留时间（可能是中途被唤醒的）
                            nanos = trip.awaitNanos(nanos);
                        }
                    }
                } catch(InterruptedException ie) {
                    if(g == generation && !g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }
                
                if(g.broken) {
                    throw new BrokenBarrierException();
                }
                
                // 当线程被唤醒后，这个栅栏已经被更新过了，所以此处表达式为true
                if(g != generation) {
                    // 返回当前线程序号，表示该线程是第parties-index个被阻塞的
                    return index;
                }
                
                // 如果在被唤醒前就自己醒了，说明这组任务失效了
                if(timed && nanos<=0L) {
                    // 破坏栅栏
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            // 释放锁(不受上面retrn的影响)
            lock.unlock();
        }
    }
    
    /**
     * Resets the barrier to its initial state.  If any parties are
     * currently waiting at the barrier, they will return with a
     * {@link BrokenBarrierException}. Note that resets <em>after</em>
     * a breakage has occurred for other reasons can be complicated to
     * carry out; threads need to re-synchronize in some other way,
     * and choose one to perform the reset.  It may be preferable to
     * instead create a new barrier for subsequent use.
     */
    // 重置栅栏
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 破坏栅栏
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Updates state on barrier trip and wakes up everyone.
     * Called only while holding lock.
     */
    // 唤醒其他parties-1个线程，生成新的栅栏，并重置计数器count
    private void nextGeneration() {
        // signal completion of last generation
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }
    
    /**
     * Returns the number of parties required to trip this barrier.
     *
     * @return the number of parties required to trip this barrier
     */
    // 返回当前可以容纳的最大线程数量
    public int getParties() {
        return parties;
    }
    
    /**
     * Returns the number of parties currently waiting at the barrier.
     * This method is primarily useful for debugging and assertions.
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    // 获取当前正在阻塞的线程数量
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Sets current barrier generation as broken and wakes up everyone.
     * Called only while holding lock.
     */
    // 破坏栅栏
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }
    
    /**
     * Queries if this barrier is in a broken state.
     *
     * @return {@code true} if one or more parties broke out of this
     * barrier due to interruption or timeout since
     * construction or the last reset, or a barrier action
     * failed due to an exception; {@code false} otherwise.
     */
    // 判断当前栅栏是否已损坏
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Each use of the barrier is represented as a generation instance.
     * The generation changes whenever the barrier is tripped, or
     * is reset. There can be many generations associated with threads
     * using the barrier - due to the non-deterministic way the lock
     * may be allocated to waiting threads - but only one of these
     * can be active at a time (the one to which {@code count} applies)
     * and all the rest are either broken or tripped.
     * There need not be an active generation if there has been a break
     * but no subsequent reset.
     */
    // 栅栏，用于标记该组线程是否有效
    private static class Generation {
        boolean broken; // 初始值为fasle，表示栅栏有效
        
        // prevent access constructor creation
        Generation() {
        }
    }
}
