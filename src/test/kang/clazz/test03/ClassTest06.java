package test.kang.clazz.test03;

import java.util.HashMap;

// 获取当前类的父类和父接口
public class ClassTest06 {
    public static void main(String[] args) {
        // 父类
        System.out.println(HashMap.class.getSuperclass());
        
        System.out.println();
        
        // 父接口
        Class[] cs = HashMap.class.getInterfaces();
        for(Class c : cs){
            System.out.println(c);
        }
    }
}
