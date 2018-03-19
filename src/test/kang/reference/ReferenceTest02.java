package test.kang.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

// 弱引用WeakReference回收测试
public class ReferenceTest02 {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<User> rq = new ReferenceQueue<>();
        
        User u1 = new User("张三");
        User u2 = new User("李四");
        
        WeakReference<User> w1 = new WeakReference<>(u1, rq);
        WeakReference<User> w2 = new WeakReference<>(u2, rq);
        
        System.out.println("开始时，u1和u2对象均存在");
        User.print(w1.get(), w2.get());
        
        // u1置为null后，u1对象只剩下w1这个弱引用。
        u1 = null;
        
        // u2对象没变化，还是有一个强引用和一个弱引用，所以它被认为是强引用
        
        System.out.println("------------执行GC------------");
        System.gc();
        Thread.sleep(1);    // 稍微暂停一下，等待GC完成
        
        System.out.println("执行GC后，弱引用w1中的u1对象被回收了，且弱引用w1进入了ReferenceQueue，w2没变化");
        User.print(w1.get(), w2.get());
        
        System.out.println("------------------------------");
    
        System.out.println("验证弱引用w1是否进入了ReferenceQueue");
        // 不断循环，直到找出目标引用
        while(true) {
            Reference r = rq.poll();    // 获取Reference（没有用remove，因为remove会导致陷入阻塞）
            
            if(r==w1){
                System.out.println("w1已进入ReferenceQueue，w1中包裹的对象：" + r.get());
                break;
            } else if(r==w2){
                System.out.println("w2已进入ReferenceQueue，w2中包裹的对象：" + r.get());
                break;
            } else if (r==null) {
                System.out.println("ReferenceQueue为空，查询结束...");
                break;
            }
        }
        // 验证结果是u1对象被回收了，w1被加入了ReferenceQueue
    }
}
