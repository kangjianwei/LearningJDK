package java.util;

/**
 * This "helper class" implements the timer's task execution thread, which
 * waits for tasks on the timer queue, executions them when they fire,
 * reschedules repeating tasks, and removes cancelled tasks and spent
 * non-repeating tasks from the queue.
 */
// 定时器线程
class TimerThread extends Thread {
    /**
     * This flag is set to false by the reaper to inform us that there are no more live references to our Timer object.
     * Once this flag is true and there are no more tasks in our queue,
     * there is no work left for us to do, so we terminate gracefully.
     * Note that this field is protected by queue's monitor!
     */
    // 定时器是否已取消（不再执行新任务）
    boolean newTasksMayBeScheduled = true;
    
    /**
     * Our Timer's queue.  We store this reference in preference to
     * a reference to the Timer so the reference graph remains acyclic.
     * Otherwise, the Timer would never be garbage-collected and this
     * thread would never go away.
     */
    // 任务队列
    private TaskQueue queue;
    
    TimerThread(TaskQueue queue) {
        this.queue = queue;
    }
    
    // 初始化定时器后，定时器线程随之启动
    public void run() {
        try {
            // 进入定时器主循环
            mainLoop();
        } finally {
            // Someone killed this Thread, behave as if Timer cancelled
            synchronized(queue) {
                newTasksMayBeScheduled = false;
                // 清空任务队列
                queue.clear();  // Eliminate obsolete references
            }
        }
    }
    
    /**
     * The main timer loop.  (See class comment.)
     */
    // 定时器主循环
    private void mainLoop() {
        for(; ; ) {
            try {
                TimerTask task;
                boolean taskFired;
                
                synchronized(queue) {
                    // Wait for queue to become non-empty
                    while(queue.isEmpty() && newTasksMayBeScheduled) {
                        // 如果任务队列为空，且定时器未取消，则阻塞定时器线程，等待任务到来
                        queue.wait();
                    }
                    
                    // 定时器线程醒来后，如果队列为空，且定时器已取消，直接退出
                    if(queue.isEmpty()) {
                        break; // Queue is empty and will forever remain; die
                    }
                    
                    /* 至此，任务队列不为空 */
                    
                    // Queue nonempty; look at first evt and do the right thing
                    long currentTime, executionTime;
    
                    // 获取队头任务
                    task = queue.getMin();
                    
                    synchronized(task.lock) {
                        // 如果该任务已被取消
                        if(task.state == TimerTask.CANCELLED) {
                            // 移除队头任务，并将触发时间最近的任务放在队头
                            queue.removeMin();
                            // 重新开始主循环
                            continue;  // No action required, poll queue again
                        }
    
                        // 任务触发时间
                        executionTime = task.nextExecutionTime;
                        
                        // 当前时间（可以近似地认为是任务本次实际触发时间）
                        currentTime = System.currentTimeMillis();
                        
                        // 如果任务可以开始执行了
                        if(taskFired = (executionTime<=currentTime)) {
                            // 一次性任务，执行完就移除
                            if(task.period == 0) {
                                // 移除队头任务，并将触发时间最近的任务放在队头
                                queue.removeMin();
                                // 任务进入【执行】状态
                                task.state = TimerTask.EXECUTED;
                            } else {
                                // 计算重复性任务的下次触发时间
                                long newTime = task.period<0
                                    ? currentTime - task.period     // 固定延时，任务下次的触发时间=任务本次实际触发时间+(-period)
                                    : executionTime + task.period;  // 固定周期，从任务初次被触发开始，以后每隔period时间就被触发一次
                                
                                // 重置队头任务的触发时间，并将其调整到队列中的合适位置
                                queue.rescheduleMin(newTime);
                            }
                        }
                    }
    
                    // 如果任务还未到触发时间，定时器线程进入阻塞
                    if(!taskFired) {
                        // Task hasn't yet fired; wait
                        queue.wait(executionTime - currentTime);
                    }
                }
    
                // 如果任务可以开始执行了
                if(taskFired) {
                    // Task fired; run it, holding no locks
                    task.run();
                }
            } catch(InterruptedException e) {
            }
        }// for(; ; )
    }
}
