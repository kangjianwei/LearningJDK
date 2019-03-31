package test.kang.clazz.test03;

// 从内部类获取外部类信息
public class ClassTest07 {
    public static void main(String[] args) {
        class 方法内部类{
        }
        
        接口07 匿名内部类对象 = new 接口07() {
        };
        
        // 测试getEnclosingClass()
        System.out.println(成员内部类.class.getEnclosingClass());
        System.out.println(方法内部类.class.getEnclosingClass());
        System.out.println(匿名内部类对象.getClass().getEnclosingClass()); // 匿名内部类
        System.out.println(接口07.class.getEnclosingClass());   // 外部类
        
        System.out.println();
        
        // 测试getDeclaringClass()
        System.out.println(成员内部类.class.getDeclaringClass());
        System.out.println(方法内部类.class.getDeclaringClass());
        System.out.println(匿名内部类对象.getClass().getDeclaringClass()); // 匿名内部类
        System.out.println(接口07.class.getDeclaringClass());   // 外部类
    }
    
    class 成员内部类{
    }
}

interface 接口07 {
}
