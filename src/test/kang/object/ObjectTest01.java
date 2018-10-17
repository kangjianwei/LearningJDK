package test.kang.object;

/*
 * 测试wait释放锁的行为
 *
 * wait会使当前线程陷入等待，并释放持有的锁让其他线程竞争
 * 为了看出效果，引入sleep做对比
 * slepp也会使线程陷入等待，但是它不释放持有的锁
 * 也就是说，在sleep期间，别的线程拿不到sleep持有的锁
 *
 * 以下callWait()、callSleep()、print方法中的代码段都持有object锁
 *
 * 测试一：在t1中调用callSleep()。
 * 此时，如果t1先拿到锁，则先等待1秒，再输出+号，最后输出-号
 * 或者，如果t2先拿到锁，则先输出-号，再等待1秒，最后输出+号
 * 注：由于main方法中先调用了t1，后调用了t2，所以t1先拿到锁的概率大得多
 *
 * 测试二：在t2中调用callWait()。
 * 此时，如果t1先拿到锁，则它释放锁的控制权，于是t2拿到锁，接着输出-号，再等待将近1秒，最后输出+号
 * 或者，如果t2先拿到锁，则顺理成章先输出-号，然后等待1秒，最后输出+号
 * 也就是说，无论如何，都是先输出-号，再等待，再输出+号
 */
public class ObjectTest01 {
    private final Object object = new Object();
    private int count = 0;
    private Thread t1 = new Thread(new Runnable() {
        @Override
        public void run() {
            callSleep(); // 测试一
            
//            callWait(); // 测试二
        }
    });
    private Thread t2 = new Thread(new Runnable() {
        @Override
        public void run() {
            synchronized(object) {
                print("-");
            }
        }
    });
    
    public static void main(String[] args) throws InterruptedException {
        ObjectTest01 ot = new ObjectTest01();
        ot.t1.start();
        ot.t2.start();
    }
    
    private void callSleep() {
        synchronized(object) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
    
            print("+");
        }
    }
    
    private void callWait() {
        synchronized(object) {
            try {
                object.wait(1000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
    
            print("+");
        }
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
