package test.kang.constructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

// 泛型形参构造器
public class ConstructorTest04 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor7 = Bean.class.getDeclaredConstructor(Object.class, Number.class, char.class);
    
        System.out.println(constructor7.getName());
        System.out.println(constructor7.toString());
        System.out.println(constructor7.toGenericString());
    
        System.out.println("\n====获取所有形参对象====");
        Parameter[] parameters = constructor7.getParameters();
        for(Parameter p : parameters){
            System.out.println(p);
        }
        
        System.out.println("\n====形参类型[类型擦除]====");
        Class[] classes = constructor7.getParameterTypes();
        for(Class c : classes){
            System.out.println(c);
        }
    
        System.out.println("\n====形参类型[支持泛型语义]====");
        Type[] types = constructor7.getGenericParameterTypes();
        for(Type t : types){
            System.out.println(t);
        }
    }
}
