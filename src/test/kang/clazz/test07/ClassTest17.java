package test.kang.clazz.test07;

import java.lang.reflect.Constructor;
import test.kang.clazz.test07.模版.子类;

// 获取当前类的public构造方法，但不包括父类中的构造方法
public class ClassTest17 {
    public static void main(String[] args) throws NoSuchMethodException {
        
        System.out.println("\n====getConstructors====");
        Constructor[] constructors = 子类.class.getConstructors();
        for (Constructor c : constructors){
            System.out.println(c);
        }
        
        System.out.println("\n====getConstructor====");
        Constructor constructor = 子类.class.getConstructor();
        System.out.println(constructor);
    }
}
