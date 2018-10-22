package test.kang.thread;

// 死锁线程无法被中断唤醒
public class ThreadTest04 {
    public static void main(String[] args) throws Exception {
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        
        Thread t1 = new Thread() {
            public void run() {
                deathLock(lock1, lock2);
            }
        };
        
        Thread t2 = new Thread() {
            public void run() {
                // 注意，这里在交换了一下位置
                deathLock(lock2, lock1);
            }
        };
        
        System.out.println("启动线程...");
        t1.start();
        t2.start();
        
        Thread.sleep(3000);
        
        System.out.println("中断线程...");
        t1.interrupt();
        t2.interrupt();
        
        // 至此，main线程可以结束了，但是线程t1和t2陷入了死锁
    }
    
    private static void deathLock(Object lock1, Object lock2) {
        try {
            synchronized(lock1) {
                Thread.sleep(10);// 不会在这里死掉
                synchronized(lock2) {// 会锁在这里，虽然阻塞了，但不会抛异常
                    System.out.println(Thread.currentThread());
                }
            }
        } catch(InterruptedException e) {
            System.out.println("解除了死锁");    // 实际上这里的死锁是无法解除的
        }
    }
}
