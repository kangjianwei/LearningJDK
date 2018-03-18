package test.kang.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

// 软引用SoftReference回收测试
public class ReferenceTest01 {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<User> rq = new ReferenceQueue<>();
    
        User u1 = new User("张三");
        User u2 = new User("李四");
    
        SoftReference<User> s1 = new SoftReference<>(u1, rq);
        SoftReference<User> s2 = new SoftReference<>(u2, rq);
    
        System.out.println("开始时，u1和u2对象均存在");
        User.print(s1.get(), s2.get());
    
        // u1置为null后，u1对象只剩下s1这个软引用。
        u1 = null;
    
        // u2对象没变化，还是有一个强引用和一个软引用，所以它被认为是强引用
    
        System.out.println("------------执行GC------------");
        System.gc();
        Thread.sleep(1);    // 稍微暂停一下，等待GC完成
        
        System.out.println("执行GC后，由于JVM堆内存充足，所以u1和u2这两个对象依然存在，s1和s2也不会进入ReferenceQueue");
        User.print(s1.get(), s2.get());
    
        System.out.println("------------------------------");
    
        System.out.println("验证软引用s1是否进入了ReferenceQueue");
        // 不断循环，直到找出目标引用
        while(true) {
            Reference r = rq.poll();    // 获取Reference（没有用remove，因为remove会导致陷入阻塞）
        
            if(r==s1){
                System.out.println("s1已进入ReferenceQueue，s1中包裹的对象：" + r.get());
                break;
            } else if(r==s2){
                System.out.println("s2已进入ReferenceQueue，s2中包裹的对象：" + r.get());
                break;
            } else if (r==null) {
                System.out.println("ReferenceQueue为空，查询结束...");
                break;
            }
        }
        // 验证结果是堆内存足够时，两个对象依然存在
    }
}
