package test.kang.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// 常规方法测试
public class MethodTest01 {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Bean bean = new Bean();
        
        Method method1 = Bean.class.getDeclaredMethod("fun");
        System.out.println(method1.invoke(bean));
    
        Method method2 = Bean.class.getDeclaredMethod("fun", int.class);
        System.out.println(method2.invoke(bean, 1));
    
        Method method3 = Bean.class.getDeclaredMethod("fun", int.class, int.class);
        System.out.println(method3.invoke(bean, 1, 2));
    
        Method method4 = Bean.class.getDeclaredMethod("fun", int.class, int.class, int.class);
        method4.setAccessible(true);    // 需要禁用安全检查
        System.out.println(method4.invoke(bean, 1, 2, 3));
    
        System.out.println("\n====方法修饰符测试====");
        System.out.println("1："+ Modifier.toString(method1.getModifiers()));
        System.out.println("2："+Modifier.toString(method2.getModifiers()));
        System.out.println("3："+Modifier.toString(method3.getModifiers()));
        System.out.println("4："+Modifier.toString(method4.getModifiers()));
    }
}
