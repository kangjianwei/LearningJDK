package test.kang.clazz.test03;

// 从内部类获取外部类信息，与getEnclosingClass()的区别是对于非内部类，返回其本身
public class ClassTest09{
    public static void main(String[] args) {
        class 方法内部类{
        }
        
        接口09 匿名内部类对象 = new 接口09() {
        };
        
        // 测试getEnclosingClass()
        System.out.println(成员内部类.class.getNestHost());
        System.out.println(方法内部类.class.getNestHost());
        System.out.println(匿名内部类对象.getClass().getNestHost()); // 匿名内部类
        System.out.println(接口09.class.getNestHost());   // 外部类
    }
    
    class 成员内部类{
    }
}

interface 接口09 {
}
