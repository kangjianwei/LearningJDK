package test.kang.method;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

// 泛型形参构造器
public class MethodTest05 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method12 = Bean.class.getDeclaredMethod("fun", Object.class, Number.class, char.class);
    
        System.out.println(method12.getName());
        System.out.println(method12.toString());
        System.out.println(method12.toGenericString());
    
        System.out.println("\n====获取所有形参对象====");
        Parameter[] parameters = method12.getParameters();
        for(Parameter p : parameters){
            System.out.println(p);
        }
    
        System.out.println("\n====形参类型[类型擦除]====");
        Class[] classes = method12.getParameterTypes();
        for(Class c : classes){
            System.out.println(c);
        }
    
        System.out.println("\n====形参类型[支持泛型语义]====");
        Type[] types = method12.getGenericParameterTypes();
        for(Type t : types){
            System.out.println(t);
        }
    }
}
