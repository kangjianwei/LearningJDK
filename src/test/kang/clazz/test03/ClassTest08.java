package test.kang.clazz.test03;

import test.kang.clazz.test03.模板.实例类;

// 从外部类获取内部类信息
public class ClassTest08 {
    public static void main(String[] args) {
        // 测试getDeclaredClasses()
        Class[] classes1 = 实例类.class.getDeclaredClasses();
        for(Class c : classes1){
            System.out.println(c.getSimpleName());
        }
        
        System.out.println();
    
        // 测试getClasses()
        Class[] classes2 = 实例类.class.getClasses();
        for(Class c : classes2){
            System.out.println(c.getSimpleName());
        }
    }
}


