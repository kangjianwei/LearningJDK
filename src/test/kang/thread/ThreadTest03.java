package test.kang.thread;

// 中断阻塞线程
public class ThreadTest03 extends Thread {
    private static int count =0;
    
    public static void main(String[] args) {
        Object object = new Object();
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    System.out.println(count++);
    
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                        System.out.println("线程t1在阻塞中收到了中断请求");
    
                        // 抛出异常相当于激活了阻塞的线程t1，在这里给线程t1设置真正的中断标记，设置成功后线程t1退出
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        
        // t2用来中断t1
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 线程处于阻塞状态时，只能收到中断通知，但无法设置中断位
                t1.interrupt();
            }
        });
        
        t1.start();
        t2.start();
    }
}
