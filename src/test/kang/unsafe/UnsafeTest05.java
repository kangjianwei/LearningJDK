package test.kang.unsafe;

import sun.misc.Unsafe;

// 线程的阻塞[park]与唤醒[unpark]
public class UnsafeTest05 {
    private static Thread t1, t2;
    
    public static void main(String[] args) {
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        
        t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 调用三次unpark，其实只会给线程t1一个许可
                unsafe.unpark(t1);
                unsafe.unpark(t1);
                unsafe.unpark(t1);
                
                // 打印a，不消费许可
                System.out.println("a");
                
                // 打印b之前线程t1先消费一个许可
                unsafe.park(false, 0);
                System.out.println("b");
                
                // 需要消费许可，但是目前已经没了，所以线程t1陷入阻塞，一直等到线程t2给了许可之后才能运行
                unsafe.park(false, 0);
                
                // 此处给许可也没用，解除不了上面的阻塞。如果t2不给许可，线程t1仍然会阻塞
                unsafe.unpark(t1);
                
                System.out.println("c");
            }
        });
    
        t2 =new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 让线程t1先跑，然后观察线程t1的阻塞
                    Thread.sleep(2000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                
                /**
                 * 等线程t1阻塞后，给线程t1一个许可，解除t1的阻塞
                 * 给的早了也不行，连续重复给的许可只算作一个
                 */
                unsafe.unpark(t1);
                
                // 使用中断也可唤醒线程t1
//                t1.interrupt();
            }
        });
        
        t1.start();
        t2.start();
    }
}
