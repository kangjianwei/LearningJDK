/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract class for fork-join tasks used to implement short-circuiting
 * stream ops, which can produce a result without processing all elements of the
 * stream.
 *
 * @param <P_IN>  type of input elements to the pipeline
 * @param <P_OUT> type of output elements from the pipeline
 * @param <R>     type of intermediate result, may be different from operation
 *                result type
 * @param <K>     type of child and sibling tasks
 *
 * @since 1.8
 */
// 用于流式运算的抽象任务，支持操作短路任务
@SuppressWarnings("serial")
abstract class AbstractShortCircuitTask<P_IN, P_OUT, R, K extends AbstractShortCircuitTask<P_IN, P_OUT, R, K>> extends AbstractTask<P_IN, P_OUT, R, K> {
    
    /**
     * The result for this computation; this is shared among all tasks and set
     * exactly once
     */
    protected final AtomicReference<R> sharedResult;  // 一个特殊的共享结果，指示任务是否应当立即完成
    
    /**
     * Indicates whether this task has been canceled.  Tasks may cancel other
     * tasks in the computation under various conditions, such as in a
     * find-first operation, a task that finds a value will cancel all tasks
     * that are later in the encounter order.
     */
    protected volatile boolean canceled;  // 指示当前任务是否已取消
    
    /**
     * Constructor for root tasks.
     *
     * @param helper      the {@code PipelineHelper} describing the stream pipeline
     *                    up to this operation
     * @param spliterator the {@code Spliterator} describing the source for this
     *                    pipeline
     */
    protected AbstractShortCircuitTask(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator) {
        super(helper, spliterator);
        sharedResult = new AtomicReference<>(null);
    }
    
    /**
     * Constructor for non-root nodes.
     *
     * @param parent      parent task in the computation tree
     * @param spliterator the {@code Spliterator} for the portion of the
     *                    computation tree described by this task
     */
    protected AbstractShortCircuitTask(K parent, Spliterator<P_IN> spliterator) {
        super(parent, spliterator);
        sharedResult = parent.sharedResult;
    }
    
    /**
     * Overrides AbstractTask version to include checks for early
     * exits while splitting or computing.
     */
    /*
     * 计算当前任务，可能需要对其进行拆分
     *
     * 这里覆盖了父类AbstractTask中的实现：
     * 每次拆分或计算任务之前，都要先检测是否已经为根任务设置好了执行结果。
     * 如果已经为根任务设置了执行结果，则结束计算过程。
     */
    @Override
    public void compute() {
        Spliterator<P_IN> rs = spliterator;
        Spliterator<P_IN> ls;
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        long sizeEstimate = rs.estimateSize();
        
        // 返回为目标结点分配的元素数量（建议值）
        long sizeThreshold = getTargetSize(sizeEstimate);
        
        boolean forkRight = false;
        
        @SuppressWarnings("unchecked")
        K task = (K) this;
        
        AtomicReference<R> sr = sharedResult;
        
        R result;
        
        // 如果还没有共享结果
        while((result = sr.get()) == null) {
            
            // 如果任务已取消，则生成一个空的结果
            if(task.taskCanceled()) {
                result = task.getEmptyResult();
                break;
            }
            
            // 如果待分割任务的数据量已满足要求，或者该任务无法再拆分，则直接生成结果
            if(sizeEstimate<=sizeThreshold || (ls = rs.trySplit()) == null) {
                result = task.doLeaf();
                break;
            }
            
            K leftChild, rightChild, taskToFork;
            
            task.leftChild = leftChild = task.makeChild(ls);
            task.rightChild = rightChild = task.makeChild(rs);
            
            // 设置挂起计数
            task.setPendingCount(1);
            
            // 轮流拆分左右子任务
            if(forkRight) {
                forkRight = false;
                rs = ls;
                task = leftChild;
                taskToFork = rightChild;
            } else {
                forkRight = true;
                task = rightChild;
                taskToFork = leftChild;
            }
            
            // 将taskToFork交给其他线程去执行
            taskToFork.fork();
            
            // 更新待分割任务中包含的元素数量
            sizeEstimate = rs.estimateSize();
        } // while
        
        // 如果是根结点，设置共享结果，否则，设置普通结果
        task.setLocalResult(result);
        
        // 尝试从task任务开始，向上传播"完成"消息
        task.tryComplete();
    }
    
    /**
     * Retrieves the local result for this task
     */
    // 返回当前任务的计算结果
    @Override
    public R getRawResult() {
        return getLocalResult();
    }
    
    /**
     * Retrieves the local result for this task.  If this task is the root,
     * retrieves the shared result instead.
     */
    // 返回当前任务的计算结果；如果是根结点，返回共享结果，否则，返回普通结果
    @Override
    public R getLocalResult() {
        if(isRoot()) {
            R answer = sharedResult.get();
            return (answer == null) ? getEmptyResult() : answer;
        } else {
            return super.getLocalResult();
        }
    }
    
    /**
     * Sets a local result for this task.  If this task is the root, set the
     * shared result instead (if not already set).
     *
     * @param localResult The result to set for this task
     */
    // 如果是根结点，设置共享结果，否则，设置普通结果
    @Override
    protected void setLocalResult(R localResult) {
        // 如果是根结点，则设置一个共享结果，以提示任务完成
        if(isRoot()) {
            if(localResult != null) {
                sharedResult.compareAndSet(null, localResult);
            }
        } else {
            super.setLocalResult(localResult);
        }
    }
    
    /**
     * Returns the value indicating the computation completed with no task
     * finding a short-circuitable result.  For example, for a "find" operation,
     * this might be null or an empty {@code Optional}.
     *
     * @return the result to return when no task finds a result
     */
    // 获取一个空的执行结果
    protected abstract R getEmptyResult();
    
    /**
     * Declares that a globally valid result has been found.  If another task has
     * not already found the answer, the result is installed in
     * {@code sharedResult}.  The {@code compute()} method will check
     * {@code sharedResult} before proceeding with computation, so this causes
     * the computation to terminate early.
     *
     * @param result the result found
     */
    // 设置共享任务结果
    protected void shortCircuit(R result) {
        if(result != null) {
            sharedResult.compareAndSet(null, result);
        }
    }
    
    /**
     * Mark this task as canceled
     */
    // 取消当前任务
    protected void cancel() {
        canceled = true;
    }
    
    /**
     * Queries whether this task is canceled.  A task is considered canceled if
     * it or any of its parents have been canceled.
     *
     * @return {@code true} if this task or any parent is canceled.
     */
    // 判断当前任务是否被取消；如果其父任务被取消，则当前任务也会被任务已取消
    protected boolean taskCanceled() {
        boolean cancel = canceled;
        if(!cancel) {
            for(K parent = getParent(); !cancel && parent != null; parent = parent.getParent()) {
                cancel = parent.canceled;
            }
        }
    
        return cancel;
    }
    
    /**
     * Cancels all tasks which succeed this one in the encounter order.
     * This includes canceling all the current task's right sibling, as well as the later right siblings of all its parents.
     */
    // 取消当前任务的所有右兄弟任务及其所有父级的右兄弟任务
    protected void cancelLaterNodes() {
        // Go up the tree, cancel right siblings of this node and all parents
    
        @SuppressWarnings("unchecked")
        K parent = getParent(), node = (K) this;
    
        while(parent != null) {
        
            // If node is a left child of parent, then has a right sibling
            if(parent.leftChild == node) {
                K rightSibling = parent.rightChild;
                if(!rightSibling.canceled) {
                    rightSibling.cancel();
                }
            }
        
            node = parent;
            parent = parent.getParent();
        }
    }
    
}
