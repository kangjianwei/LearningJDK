package test.kang.clazz.test06;

import java.lang.reflect.Method;
import test.kang.clazz.test06.模版.实例类;

// 获取public方法，包括父类/父接口中的public方法
public class ClassTest14 {
    public static void main(String[] args) throws NoSuchMethodException {
    
        // 返回当前类中所有public方法，包括父类/父接口中的public方法
        System.out.println("\n====getMethods====");
        Method[] methods = 实例类.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    
        // 返回当前类中指定名称和形参的public方法，包括父类/父接口中的public方法
        System.out.println("\n====getMethod====");
        Method method1 = 实例类.class.getMethod("实例类方法_public");
        System.out.println(method1.getName());
    }
}
