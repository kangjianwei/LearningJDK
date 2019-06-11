package java.util;

/**
 * This class represents a timer task queue: a priority queue of TimerTasks,
 * ordered on nextExecutionTime.  Each Timer object has one of these, which it
 * shares with its TimerThread.  Internally this class uses a heap, which
 * offers log(n) performance for the add, removeMin and rescheduleMin
 * operations, and constant time performance for the getMin operation.
 */
// 定时器任务队列
class TaskQueue {
    /**
     * Priority queue represented as a balanced binary heap: the two children
     * of queue[n] are queue[2*n] and queue[2*n+1].  The priority queue is
     * ordered on the nextExecutionTime field: The TimerTask with the lowest
     * nextExecutionTime is in queue[1] (assuming the queue is nonempty).  For
     * each node n in the heap, and each descendant of n, d,
     * n.nextExecutionTime <= d.nextExecutionTime.
     */
    // 任务队列，实际存储任务的地方，索引0处空闲
    private TimerTask[] queue = new TimerTask[128];
    
    /**
     * The number of tasks in the priority queue.  (The tasks are stored in
     * queue[1] up to queue[size]).
     */
    // 任务数量
    private int size = 0;
    
    /**
     * Adds a new task to the priority queue.
     */
    // 将任务送入任务队列排队
    void add(TimerTask task) {
        // Grow backing store if necessary
        if(size + 1 == queue.length) {
            // 扩容
            queue = Arrays.copyOf(queue, 2 * queue.length);
        }
        
        queue[++size] = task;
    
        // 调整size处的任务到队列中的合适位置
        fixUp(size);
    }
    
    /**
     * Return the "head task" of the priority queue.
     * (The head task is an task with the lowest nextExecutionTime.)
     */
    // 获取队头任务
    TimerTask getMin() {
        return queue[1];
    }
    
    /**
     * Return the ith task in the priority queue, where i ranges from 1 (the
     * head task, which is returned by getMin) to the number of tasks on the
     * queue, inclusive.
     */
    // 获取索引i处的任务
    TimerTask get(int i) {
        return queue[i];
    }
    
    /**
     * Remove the head task from the priority queue.
     */
    // 移除队头任务，并将触发时间最近的任务放在队头
    void removeMin() {
        // 先将队尾任务放到队头
        queue[1] = queue[size];
        queue[size--] = null;  // Drop extra reference to prevent memory leak
        // 调整当前队头任务（之前的队尾任务）到队列中合适的位置
        fixDown(1);
    }
    
    /**
     * Removes the ith element from queue without regard for maintaining
     * the heap invariant.  Recall that queue is one-based, so
     * 1 <= i <= size.
     */
    // 快速移除索引i处的任务（没有重建小顶堆）
    void quickRemove(int i) {
        assert i<=size;
        
        queue[i] = queue[size];
        queue[size--] = null;  // Drop extra ref to prevent memory leak
    }
    
    /**
     * Sets the nextExecutionTime associated with the head task to the
     * specified value, and adjusts priority queue accordingly.
     */
    // 重置队头任务的触发时间，并将其调整到队列中的合适位置
    void rescheduleMin(long newTime) {
        // 重置队头任务的触发时间
        queue[1].nextExecutionTime = newTime;
        // 将该任务调整到队列中的合适位置
        fixDown(1);
    }
    
    /**
     * Removes all elements from the priority queue.
     */
    // 清空任务队列
    void clear() {
        // Null out task references to prevent memory leak
        for(int i = 1; i<=size; i++) {
            queue[i] = null;
        }
        
        size = 0;
    }
    
    /**
     * Returns true if the priority queue contains no elements.
     */
    // 判断队列是否为空
    boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns the number of tasks currently on the queue.
     */
    // 返回队列长度
    int size() {
        return size;
    }
    
    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     */
    // 重建小顶堆
    void heapify() {
        for(int i = size / 2; i >= 1; i--) {
            fixDown(i);
        }
    }
    
    /**
     * Establishes the heap invariant (described above) assuming the heap
     * satisfies the invariant except possibly for the leaf-node indexed by k
     * (which may have a nextExecutionTime less than its parent's).
     *
     * This method functions by "promoting" queue[k] up the hierarchy
     * (by swapping it with its parent) repeatedly until queue[k]'s
     * nextExecutionTime is greater than or equal to that of its parent.
     */
    // 插入。需要从小顶堆的结点k开始，向【上】查找一个合适的位置插入原k索引处的任务
    private void fixUp(int k) {
        while(k>1) {
            // 获取父结点索引
            int j = k >> 1;
    
            // 如果待插入元素大于父节点中的元素，则退出循环
            if(queue[k].nextExecutionTime>=queue[j].nextExecutionTime) {
                break;
            }
    
            // 子结点保存父结点中的元素
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;
    
            // 向上搜寻合适的插入位置
            k = j;
        }
    }
    
    /**
     * Establishes the heap invariant (described above) in the subtree
     * rooted at k, which is assumed to satisfy the heap invariant except
     * possibly for node k itself (which may have a nextExecutionTime greater
     * than its children's).
     *
     * This method functions by "demoting" queue[k] down the hierarchy
     * (by swapping it with its smaller child) repeatedly until queue[k]'s
     * nextExecutionTime is less than or equal to those of its children.
     */
    // 插入。需要从小顶堆的结点k开始，向【下】查找一个合适的位置插入原k索引处的任务
    private void fixDown(int k) {
        int j;
        
        while((j = k << 1)<=size && j>0) {
            // 让j存储子结点中较小结点的索引
            if(j<size && queue[j].nextExecutionTime>queue[j + 1].nextExecutionTime) {
                j++; // j indexes smallest kid
            }
    
            // 如果待插入元素小于子结点中较小的元素，则退出循环
            if(queue[k].nextExecutionTime<=queue[j].nextExecutionTime) {
                break;
            }
    
            // 父结点位置保存子结点中较小的元素
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;
    
            // 向下搜寻合适的插入位置
            k = j;
        }
    }
}

