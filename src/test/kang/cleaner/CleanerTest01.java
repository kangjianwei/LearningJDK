package test.kang.cleaner;

import java.lang.ref.Cleaner;

/*
 * 测试Cleaner的行为
 *
 * 注：此案例运行在JDK>=9中。
 * 如果是JDK<9，需要导包sun.misc.Cleaner，且使用另外一种创建Cleaner的方式（创建和注册结合在一起）
 */
public class CleanerTest01 {
    static Object o = new Object();
    
    public static void main(String[] args) throws InterruptedException {
        // 创建清理器，内部启动清理服务
        Cleaner cleaner = Cleaner.create();
        
        // 注册追踪的对象和清理动作
        cleaner.register(o, new Runnable() {
            @Override
            public void run() {
                System.out.println("对象已被清理！");
            }
        });
        
        o = null;   // 去掉对象的强引用
        
        System.gc();
        
        Thread.sleep(1);    // 给gc一点时间
    }
}
