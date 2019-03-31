package test.kang.clazz.test06;

import java.lang.reflect.Method;

interface 内部接口 {
}

// 获取内部类所处的外部方法
public class ClassTest16 {
    class 成员内部类 {
    }
    
    内部接口 obj1 = new 内部接口() {
        // 匿名成员内部类对象
    };
    
    
    public static void main(String[] args) {
        ClassTest16 test = new ClassTest16();
        
        test.fun1();
        test.fun2();
        test.fun3();
        test.fun4();
    }
    
    public void fun1() {
        class 方法内部类 {
        }
        
        方法内部类 obj = new 方法内部类();
        
        /*
         * 这里正常运行
         * 因为用来声明obj对象的方法内部类定义在fun1()方法内部
         */
        System.out.println("    方法内部类的外部方法：" + obj.getClass().getEnclosingMethod().getName());
    }
    
    public void fun2() {
        内部接口 obj2 = new 内部接口() {
            // 匿名方法内部类对象
        };
        
        /*
         * 这里正常运行
         * 因为用来声明obj2对象的匿名类定义在fun2()方法内部
         */
        System.out.println("匿名方法内部类的外部方法：" + obj2.getClass().getEnclosingMethod().getName());
    }
    
    public void fun3() {
        成员内部类 obj = new 成员内部类();
        
        /*
         * 这里返回null，因为用来声明obj对象的成员内部类定义在所有方法外面
         */
        Method method = obj.getClass().getEnclosingMethod();
        if(method == null) {
            System.out.println("    成员内部类的外部方法：" + "null");
        } else {
            System.out.println("    成员内部类的外部方法：" + method.getName());
        }
    }
    
    public void fun4() {
        /*
         * 这里返回null，因为用来声明obj1对象的匿名类定义在所有方法外面
         */
        Method method = obj1.getClass().getEnclosingMethod();
        if(method == null) {
            System.out.println("匿名成员内部类的外部方法：" + "null");
        } else {
            System.out.println("匿名成员内部类的外部方法：" + method.getName());
        }
    }
}
