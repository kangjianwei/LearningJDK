package test.kang.threadlocal;

// ThreadLocal可用于共享数据到当前线程
public class ThreadLocalTest02 {
    private static int age = 20;
    
    public static void main(String[] args) {
        ThreadLocal<User> userThreadLocal = new ThreadLocal<>();
        
        User user = new User("张三", age);
        
        userThreadLocal.set(user);
        
        // 循环次数少了可能效果不明显
        for(int i=0; i<1000; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread currentThread = Thread.currentThread();
                    userThreadLocal.set(user);  // 为当前线程设置user
                    userThreadLocal.get().setAge(++age);    // 修改年龄
                    
                    // 查看每个子线程中的user信息
                    System.out.println(currentThread+"  "+userThreadLocal.get());
                }
            }).start();
        }
        
        // 给子线程足够的时间去完成计算
        try {
            Thread.sleep(3000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        
        /*
         * 查看main线程中的user
         * 按道理，上面循环了1000次，age累加1000次，这里的年龄应该是120
         * 实际上，多线程会破坏数据的原子性，这里的age计算值可能小于120
         */
        System.out.println("主线程"+"  "+userThreadLocal.get());
        
        /*
         * 测试结果：
         * 1. 各线程共享user，在一个线程中修改user，另一个线程可以看到修改
         * 2. ThreadLocal不能保证数据的原子性
         */
    }
}
