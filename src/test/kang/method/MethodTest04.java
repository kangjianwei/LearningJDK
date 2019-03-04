package test.kang.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// 形参数量可变的方法
public class MethodTest04 {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method11 = Bean.class.getDeclaredMethod("fun", int[].class);
        
        method11.invoke(new Bean(), (Object) new int[]{1, 2, 3});   // 调用参数数量可变的方法
    
        System.out.println(method11.getName());
        System.out.println(method11.toString());
        System.out.println(method11.toGenericString());
    
        System.out.println();
    
        System.out.println(method11.getParameterCount());   // 参数数量
        System.out.println(method11.isVarArgs());           // 是否为可变数量形参
    }
}
