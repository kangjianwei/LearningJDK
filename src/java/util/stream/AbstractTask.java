/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Abstract base class for most fork-join tasks used to implement stream ops.
 * Manages splitting logic, tracking of child tasks, and intermediate results.
 * Each task is associated with a {@link Spliterator} that describes the portion
 * of the input associated with the subtree rooted at this task.
 * Tasks may be leaf nodes (which will traverse the elements of
 * the {@code Spliterator}) or internal nodes (which split the
 * {@code Spliterator} into multiple child tasks).
 *
 * @param <P_IN>  Type of elements input to the pipeline
 * @param <P_OUT> Type of elements output from the pipeline
 * @param <R>     Type of intermediate result, which may be different from operation
 *                result type
 * @param <K>     Type of parent, child and sibling tasks
 *
 * @implNote <p>This class is based on {@link CountedCompleter}, a form of fork-join task
 * where each task has a semaphore-like count of uncompleted children, and the
 * task is implicitly completed and notified when its last child completes.
 * Internal node tasks will likely override the {@code onCompletion} method from
 * {@code CountedCompleter} to merge the results from child tasks into the
 * current task's result.
 *
 * <p>Splitting and setting up the child task links is done by {@code compute()}
 * for internal nodes.  At {@code compute()} time for leaf nodes, it is
 * guaranteed that the parent's child-related fields (including sibling links
 * for the parent's children) will be set up for all children.
 *
 * <p>For example, a task that performs a reduce would override {@code doLeaf()}
 * to perform a reduction on that leaf node's chunk using the
 * {@code Spliterator}, and override {@code onCompletion()} to merge the results
 * of the child tasks for internal nodes:
 *
 * <pre>{@code
 *     protected S doLeaf() {
 *         spliterator.forEach(...);
 *         return localReductionResult;
 *     }
 *
 *     public void onCompletion(CountedCompleter caller) {
 *         if (!isLeaf()) {
 *             ReduceTask<P_IN, P_OUT, T, R> child = children;
 *             R result = child.getLocalResult();
 *             child = child.nextSibling;
 *             for (; child != null; child = child.nextSibling)
 *                 result = combine(result, child.getLocalResult());
 *             setLocalResult(result);
 *         }
 *     }
 * }</pre>
 *
 * <p>Serialization is not supported as there is no intention to serialize
 * tasks managed by stream ops.
 * @since 1.8
 */
// 用于流式运算的抽象任务，依托于fork/join框架，可并行地执行该任务
@SuppressWarnings("serial")
abstract class AbstractTask<P_IN, P_OUT, R, K extends AbstractTask<P_IN, P_OUT, R, K>> extends CountedCompleter<R> {
    
    // 默认的子任务数量
    private static final int LEAF_TARGET = ForkJoinPool.getCommonPoolParallelism() << 2;
    
    /** The pipeline helper, common to all tasks in a computation */
    protected final PipelineHelper<P_OUT> helper;   // 流
    
    /**
     * The spliterator for the portion of the input associated with the subtree rooted at this task
     */
    protected Spliterator<P_IN> spliterator;    // 流迭代器
    
    /** Target leaf size, common to all tasks in a computation */
    protected long targetSize;     // 目标叶子大小
    
    /**
     * The left child.
     * null if no children
     * if non-null rightChild is non-null
     */
    protected K leftChild;    // 左孩子
    
    /**
     * The right child.
     * null if no children
     * if non-null leftChild is non-null
     */
    protected K rightChild;   // 右孩子
    
    /** The result of this node, if completed */
    private R localResult;    // 当前任务的执行结果
    
    /**
     * Constructor for root nodes.
     *
     * @param helper      The {@code PipelineHelper} describing the stream pipeline
     *                    up to this operation
     * @param spliterator The {@code Spliterator} describing the source for this
     *                    pipeline
     */
    protected AbstractTask(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator) {
        super(null);
        this.helper = helper;
        this.spliterator = spliterator;
        this.targetSize = 0L;
    }
    
    /**
     * Constructor for non-root nodes.
     *
     * @param parent      this node's parent task
     * @param spliterator {@code Spliterator} describing the subtree rooted at
     *                    this node, obtained by splitting the parent {@code Spliterator}
     */
    protected AbstractTask(K parent, Spliterator<P_IN> spliterator) {
        super(parent);
        this.spliterator = spliterator;
        this.helper = parent.helper;
        this.targetSize = parent.targetSize;
    }
    
    /**
     * Constructs a new node of type T whose parent is the receiver; must call
     * the AbstractTask(T, Spliterator) constructor with the receiver and the
     * provided Spliterator.
     *
     * @param spliterator {@code Spliterator} describing the subtree rooted at
     *                    this node, obtained by splitting the parent {@code Spliterator}
     *
     * @return newly constructed child node
     */
    // 返回一个子任务，该子任务的数据源是spliterator，以待处理
    protected abstract K makeChild(Spliterator<P_IN> spliterator);
    
    /**
     * Decides whether or not to split a task further or compute it
     * directly. If computing directly, calls {@code doLeaf} and pass
     * the result to {@code setRawResult}. Otherwise splits off
     * subtasks, forking one and continuing as the other.
     *
     * <p> The method is structured to conserve resources across a
     * range of uses.  The loop continues with one of the child tasks
     * when split, to avoid deep recursion. To cope with spliterators
     * that may be systematically biased toward left-heavy or
     * right-heavy splits, we alternate which child is forked versus
     * continued in the loop.
     */
    // 计算当前任务，可能需要对其进行拆分
    @Override
    public void compute() {
        Spliterator<P_IN> rightSplit = spliterator;
        Spliterator<P_IN> leftSplit;
    
        // 获取right任务剩余元素数量
        long rightSplitSize = rightSplit.estimateSize();
    
        // 返回为目标结点分配的元素数量（建议值）
        long sizeThreshold = getTargetSize(rightSplitSize);
    
        boolean forkRight = false;
    
        @SuppressWarnings("unchecked")
        K task = (K) this;
    
        while(true) {
        
            // 如果right任务的数据量已经满足要求，则无需再拆分
            if(rightSplitSize<=sizeThreshold) {
                break;
            }
        
            // 如果right任务数据量过大，则需要拆分其流迭代器
            leftSplit = rightSplit.trySplit();
            if(leftSplit == null) {
                break;
            }
        
            K leftChild, rightChild, taskToFork;
        
            // 封装左右孩子任务
            task.leftChild = leftChild = task.makeChild(leftSplit);
            task.rightChild = rightChild = task.makeChild(rightSplit);
        
            // 设置挂起计数
            task.setPendingCount(1);
        
            // 轮流拆分左右子任务
            if(forkRight) {
                forkRight = false;
                rightSplit = leftSplit;
                task = leftChild;
                taskToFork = rightChild;
            } else {
                forkRight = true;
                task = rightChild;
                taskToFork = leftChild;
            }
        
            // 将taskToFork交给其他线程去执行
            taskToFork.fork();
        
            // 更新right任务中包含的元素数量
            rightSplitSize = rightSplit.estimateSize();
        }
    
        // 当前线程直接执行最后剩余的子任务，并返回子任务的计算结果
        R result = task.doLeaf();
    
        // 设置task任务的计算结果
        task.setLocalResult(result);
    
        // 尝试从task任务开始，向上传播"完成"消息
        task.tryComplete();
    }
    
    /**
     * {@inheritDoc}
     *
     * @implNote Clears spliterator and children fields.  Overriders MUST call
     * {@code super.onCompletion} as the last thing they do if they want these
     * cleared.
     */
    /*
     * 在当前(子)任务执行完成后，需要执行该回调方法。
     *
     * 参数中的caller是促进当前任务完成的子任务(只有子任务完成了，父任务才可能完成)。
     * 如果当前任务没有子任务，或者并不关心子任务，则参数caller的值可以直接传入当前任务。
     *
     * 注：该方法在complete()或tryComplete()中被回调
     */
    @Override
    public void onCompletion(CountedCompleter<?> caller) {
        // 置空当前任务内的核心参数
        spliterator = null;
        leftChild = rightChild = null;
    }
    
    /**
     * Computes the result associated with a leaf node.  Will be called by
     * {@code compute()} and the result passed to @{code setLocalResult()}
     *
     * @return the computed result of a leaf node
     */
    // 返回子任务的计算结果
    protected abstract R doLeaf();
    
    /**
     * Default target of leaf tasks for parallel decomposition.
     * To allow load balancing, we over-partition, currently to approximately
     * four tasks per processor, which enables others to help out
     * if leaf tasks are uneven or some processors are otherwise busy.
     */
    // 如果需要拆分当前任务，则返回子任务数量的一个建议值
    public static int getLeafTarget() {
        Thread t = Thread.currentThread();
        if(t instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).getPool().getParallelism() << 2;
        }
        
        return LEAF_TARGET;
    }
    
    /**
     * Returns a suggested target leaf size based on the initial size estimate.
     *
     * @return suggested target leaf size
     */
    // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
    public static long suggestTargetSize(long sizeEstimate) {
        long est = sizeEstimate / getLeafTarget();
        return est>0L ? est : 1L;
    }
    
    /**
     * Returns the targetSize, initializing it via the supplied size estimate if not already initialized.
     */
    // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
    protected final long getTargetSize(long sizeEstimate) {
        if(targetSize != 0) {
            return targetSize;
        }
        
        // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
        targetSize = suggestTargetSize(sizeEstimate);
        
        return targetSize;
    }
    
    /**
     * Returns the local result, if any. Subclasses should use
     * {@link #setLocalResult(Object)} and {@link #getLocalResult()} to manage
     * results.  This returns the local result so that calls from within the
     * fork-join framework will return the correct result.
     *
     * @return local result for this node previously stored with
     * {@link #setLocalResult}
     */
    // 返回当前任务的计算结果
    @Override
    public R getRawResult() {
        return localResult;
    }
    
    /**
     * Does nothing; instead, subclasses should use
     * {@link #setLocalResult(Object)}} to manage results.
     *
     * @param result must be null, or an exception is thrown (this is a safety
     *               tripwire to detect when {@code setRawResult()} is being used
     *               instead of {@code setLocalResult()}
     */
    // 设置当前任务的计算结果
    @Override
    protected void setRawResult(R result) {
        if(result != null) {
            throw new IllegalStateException();
        }
    }
    
    /**
     * Retrieves a result previously stored with {@link #setLocalResult}
     *
     * @return local result for this node previously stored with
     * {@link #setLocalResult}
     */
    // 返回当前任务的计算结果
    protected R getLocalResult() {
        return localResult;
    }
    
    /**
     * Associates the result with the task, can be retrieved with
     * {@link #getLocalResult}
     *
     * @param localResult local result for this node
     */
    // 设置当前任务的计算结果
    protected void setLocalResult(R localResult) {
        this.localResult = localResult;
    }
    
    /**
     * Returns the parent of this task, or null if this task is the root
     *
     * @return the parent of this task, or null if this task is the root
     */
    // 判断是否为父任务
    @SuppressWarnings("unchecked")
    protected K getParent() {
        return (K) getCompleter();
    }
    
    /**
     * Indicates whether this task is a leaf node.  (Only valid after
     * {@link #compute} has been called on this node).  If the node is not a
     * leaf node, then children will be non-null and numChildren will be
     * positive.
     *
     * @return {@code true} if this task is a leaf node
     */
    /*
     * 判断当前任务是否为叶子任务
     *
     * 如果不是叶子任务，则leftChild和rightChild必定都不为空，
     * 参见上面的onCompletion()方法。
     */
    protected boolean isLeaf() {
        return leftChild == null;
    }
    
    /**
     * Indicates whether this task is the root node
     *
     * @return {@code true} if this task is the root node.
     */
    // 判断当前任务是否为根任务
    protected boolean isRoot() {
        return getParent() == null;
    }
    
    /**
     * Returns whether this node is a "leftmost" node -- whether the path from
     * the root to this node involves only traversing leftmost child links.  For
     * a leaf node, this means it is the first leaf node in the encounter order.
     *
     * @return {@code true} if this node is a "leftmost" node
     */
    // 判断是否为最左侧的任务
    protected boolean isLeftmostNode() {
        @SuppressWarnings("unchecked")
        K node = (K) this;
    
        while(node != null) {
            K parent = node.getParent();
        
            if(parent != null && parent.leftChild != node) {
                return false;
            }
        
            node = parent;
        }
        
        return true;
    }
    
}
