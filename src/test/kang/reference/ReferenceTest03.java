package test.kang.reference;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;

// 虚引用PhantomReference回收测试
public class ReferenceTest03 {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<User> rq = new ReferenceQueue<>();
    
        User u1 = new User("张三");
        User u2 = new User("李四");
        
        PhantomReference<User> p1 = new PhantomReference<>(u1, rq);
        PhantomReference<User> p2 = new PhantomReference<>(u2, rq);
        
        System.out.println("开始时，u1和u2对象均存在");
        // 虚引用禁止通过Reference中的get方法获取追踪的对象，所以这里使用反射来获取
        getUserFrom(p1);
        getUserFrom(p2);
    
        // u1置为null后，u1对象只剩下p1这个虚引用。
        u1 = null;
        
        // u2对象没变化，还是有一个强引用和一个虚引用，所以它被认为是强引用
    
        System.out.println("------------执行GC------------");
        System.gc();
        Thread.sleep(1);    // 稍微暂停一下，等待GC完成
    
        System.out.println("当前JDK版本："+ System.getProperty("java.version"));
        System.out.println("在JDK9之前，执行GC后，u1和u2这两个对象依然存在，但是虚引用p1进入了ReferenceQueue，p2没变化");
        System.out.println("从JDK9开始，执行GC后，虚引用p1中的u1对象被回收了，且虚引用p1进入了ReferenceQueue，p2没变化");
        getUserFrom(p1);
        getUserFrom(p2);
        
        System.out.println("------------------------------");
    
        System.out.println("验证虚引用p1是否进入了ReferenceQueue");
        // 不断循环，直到找出目标引用
        while(true) {
            Reference r = rq.poll();    // 获取Reference（没有用remove，因为remove会导致陷入阻塞）
            
            /*
             * 在JDK9之前，虚引用包裹的对象不会被自动释放，这里有值。
             * 从JDK9开始，其行为与弱引用一致，这里输出null。
             */
            if(r==p1){
                System.out.print("p1已进入ReferenceQueue，p1中包裹的对象：");
                getUserFrom(r);
                break;
            } else if(r==p2){
                System.out.print("p2已进入ReferenceQueue，p2中包裹的对象：");
                getUserFrom(r);
                break;
            } else if(r==null) {
                System.out.println("ReferenceQueue为空，查询结束...");
                break;
            }
        }
        // 验证结果是p1被加入了ReferenceQueue，但u1有没有被回收，取决于是JDK9之前（不会回收）还是之后（会回收）
    }
    
    // 通过反射从Reference中获取其包裹的对象，并打印
    private static void getUserFrom(Reference r){
        try {
            Field rereferent = Reference.class.getDeclaredField("referent");
            rereferent.setAccessible(true);
            User u = (User)rereferent.get(r);
            User.print(u);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
