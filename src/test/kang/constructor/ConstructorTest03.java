package test.kang.constructor;

import java.lang.reflect.Constructor;

// 形参数量可变的构造器
public class ConstructorTest03 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor6 = Bean.class.getDeclaredConstructor(int[].class);
    
        System.out.println(constructor6.getName());
        System.out.println(constructor6.toString());
        System.out.println(constructor6.toGenericString());
    
        System.out.println();
        
        System.out.println(constructor6.getParameterCount());   // 参数数量
        System.out.println(constructor6.isVarArgs());           // 是否为可变数量形参
    }
}
