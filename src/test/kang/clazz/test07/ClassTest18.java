package test.kang.clazz.test07;

import java.lang.reflect.Constructor;
import test.kang.clazz.test07.模版.子类;

// 获取当前类的构造方法，但不包括父类中的构造方法
public class ClassTest18 {
    public static void main(String[] args) throws NoSuchMethodException {
        
        System.out.println("\n====getDeclaredConstructors====");
        Constructor[] constructors = 子类.class.getDeclaredConstructors();
        for (Constructor c : constructors){
            System.out.println(c);
        }
        
        System.out.println("\n====getDeclaredConstructor====");
        Constructor constructor = 子类.class.getDeclaredConstructor();
        System.out.println(constructor);
    }
}
