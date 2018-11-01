package test.kang.thread;

/*
 * 测试守护线程的行为
 *
 * 守护线程要么在自己的run方法执行完后结束，要么在其他所有线程都完成后自身也结束。
 *
 * 以下示例中，将t设为守护线程。
 * 只要main线程仍然存活，t线程就会每隔大概2秒就输出一次。
 * 当main线程结束后，t线程也会跟着结束。
 *
 * 如果t不是守护线程，那么即使main线程死亡，t线程也会一直每隔大概2秒就输出一次。
 */
public class ThreadTest01 {
    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            int count = 0;
            
            @Override
            public void run() {
                // 死循环
                while(true){
                    System.out.println("Hello "+count++);
    
                    try {
                        // 休眠2秒
                        Thread.sleep(2000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    
        // 作为对比，可以尝试注释掉此语句
        t.setDaemon(true); // 将t设置为守护线程
        t.start();
        
        try {
            // 主线程休眠5秒
            Thread.sleep(5000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("main线程结束后，作为守护线程的子线程也将退出");
    }
}
