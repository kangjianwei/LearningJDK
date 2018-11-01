package test.kang.thread;

// 中断非阻塞线程
public class ThreadTest02 {
    private static int count =0;
    
    public static void main(String[] args) {
        Object object = new Object();
    
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<10000&&!Thread.currentThread().isInterrupted(); i++) {
                    System.out.println(count++);
                }
            }
        });
        
        // t2用来中断t1
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
    
                try {
                    Thread.sleep(10);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
    
                t1.interrupt();
            }
        });
        
        t1.start();
        t2.start();
    }
}
