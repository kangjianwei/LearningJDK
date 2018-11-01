package test.kang.object;

// 测试wait和notify的配套使用
public class ObjectTest02 {
    private final Object object = new Object();
    private int count = 0;
    private Thread t1 = new Thread(new Runnable() {
        @Override
        public void run() {
            synchronized(object) {
                try {
//                    object.wait();
                    object.wait(5000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
        
                print("+");
            }
        }
    });
    private Thread t2 = new Thread(new Runnable() {
        @Override
        public void run() {
            synchronized(object) {
                print("-");
                
                object.notify();
//                object.notifyAll();
            }
        }
    });
    
    public static void main(String[] args) throws InterruptedException {
        ObjectTest02 ot = new ObjectTest02();
        ot.t1.start();
        ot.t2.start();
    }
    
    private void print(String s) {
        for(int i = 0; i<500; i++) {
            System.out.print(s);
            if(++count % 100 == 0) {
                System.out.println();
            }
        }
    }
}
