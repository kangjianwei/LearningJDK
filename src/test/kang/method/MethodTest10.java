package test.kang.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;

// 获取返回类型处的【被注解类型】
public class MethodTest10 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method14 = Bean.class.getDeclaredMethod("fun", short.class, short.class, Object.class);
    
        AnnotatedType at = method14.getAnnotatedReturnType();
    
        System.out.println("方法的返回类型：");
        System.out.println(at.getType());
    
        System.out.println();
    
        System.out.println("【被注解类型】上的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
