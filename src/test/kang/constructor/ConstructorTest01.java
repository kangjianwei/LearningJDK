package test.kang.constructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

// 常规构造器测试
public class ConstructorTest01 {
    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Bean> constructor1 = Bean.class.getDeclaredConstructor();
        Bean bean1 = constructor1.newInstance();
        System.out.println(constructor1.getName()+"  "+bean1);
        
        Constructor<Bean> constructor2 = Bean.class.getDeclaredConstructor(int.class);
        Bean bean2 = constructor2.newInstance(1);
        System.out.println(constructor2.getName()+"  "+bean2);
        
        Constructor<Bean> constructor3 = Bean.class.getDeclaredConstructor(int.class, int.class);
        Bean bean3 = constructor3.newInstance(1, 2);
        System.out.println(constructor3.getName()+"  "+bean3);
        
        Constructor<Bean> constructor4 = Bean.class.getDeclaredConstructor(int.class, int.class, int.class);
        constructor4.setAccessible(true);   // 需要禁用安全检查
        Bean bean4 = constructor4.newInstance(1, 2, 3);
        System.out.println(constructor4.getName()+"  "+bean4);
        
        
        System.out.println("\n====构造器修饰符测试====");
        System.out.println("1："+Modifier.toString(constructor1.getModifiers()));
        System.out.println("2："+Modifier.toString(constructor2.getModifiers()));
        System.out.println("3："+Modifier.toString(constructor3.getModifiers()));
        System.out.println("4："+Modifier.toString(constructor4.getModifiers()));
    }
}
