package test.kang.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;

// 获取Receiver Type上的【被注解类型】
public class MethodTest11 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method15 = Bean.class.getDeclaredMethod("fun", byte.class);
    
        AnnotatedType at = method15.getAnnotatedReceiverType();
    
        System.out.println("Receiver Type：");
        System.out.println(at.getType());
    
        System.out.println();
    
        System.out.println("关联的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
