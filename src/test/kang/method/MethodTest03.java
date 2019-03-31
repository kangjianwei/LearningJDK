package test.kang.method;

import java.lang.reflect.Method;

// 获取返回值类型
public class MethodTest03 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method6 = Bean.class.getDeclaredMethod("fun", boolean.class);
        Method method7 = Bean.class.getDeclaredMethod("fun", boolean.class, boolean.class);
        Method method8 = Bean.class.getDeclaredMethod("fun", boolean.class, boolean.class, boolean.class);
        Method method9 = Bean.class.getDeclaredMethod("fun", boolean.class, boolean.class, boolean.class, boolean.class);
        Method method10 = Bean.class.getDeclaredMethod("fun", boolean.class, boolean.class, boolean.class, boolean.class, boolean.class);
        
        System.out.println("\n====获取返回值类型[类型擦除]====");
        System.out.println(method6.getReturnType());
        System.out.println(method7.getReturnType());
        System.out.println(method8.getReturnType());
        System.out.println(method9.getReturnType());
        System.out.println(method10.getReturnType());
    
        System.out.println("\n====获取返回值类型[支持泛型语义]====");
        System.out.println(method6.getGenericReturnType());
        System.out.println(method7.getGenericReturnType());
        System.out.println(method8.getGenericReturnType());
        System.out.println(method9.getGenericReturnType());
        System.out.println(method10.getGenericReturnType());
    }
}
