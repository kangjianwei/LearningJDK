package test.kang.reference;

import java.lang.ref.ReferenceQueue;

// 自定义引用CustomReference回收测试
public class ReferenceTest04 {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<User> rq = new ReferenceQueue<>();
        
        /* 注意IDCard对象全程不受影响 */
        
        User u1 = new User("张三");
        IDCard id1 = new IDCard(123456789);
    
        User u2 = new User("李四");
        IDCard id2 = new IDCard(987654321);
    
        CustomReference<User> c1 = new CustomReference<>(id1, u1, rq);
        CustomReference<User> c2 = new CustomReference<>(id2, u2, rq);
        
        System.out.println("开始时，u1和u2对象均存在");
        System.out.println(c1.getKey()+""+c1.get());
        System.out.println(c2.getKey()+""+c2.get());
        
        // u1置为null后，u1对象只剩下c1这个弱引用。
        u1 = null;
        
        // u2对象没变化，还是有一个强引用和一个弱引用，所以它被认为是强引用
        
        System.out.println("------------执行GC------------");
        System.gc();
        Thread.sleep(1);    // 稍微暂停一下，等待GC完成
    
        System.out.println("执行GC后，弱引用c1中的u1对象被回收了，且弱引用c1进入了ReferenceQueue，c2没变化");
        System.out.println(c1.getKey()+""+c1.get());
        System.out.println(c2.getKey()+""+c2.get());

        System.out.println("------------------------------");
    
        System.out.println("验证弱引用c1是否进入了ReferenceQueue");
        // 不断循环，直到找出目标引用
        while(true) {
            CustomReference r = (CustomReference)rq.poll();    // 获取Reference（没有用remove，因为remove会导致陷入阻塞）
        
            if(r==c1){
                System.out.println("c1已进入ReferenceQueue：" + c1.getKey()+""+c1.get());
                break;
            } else if(r==c2){
                System.out.println("c2已进入ReferenceQueue：" + c2.getKey()+""+c2.get());
                break;
            } else if (r==null) {
                System.out.println("ReferenceQueue为空，查询结束...");
                break;
            }
        }
    }
}
