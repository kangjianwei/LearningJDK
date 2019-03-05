package test.kang.method;

import test.kang.method.annotation.可重复注解;
import test.kang.method.annotation.注解_METHOD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

// 返回所有注解，或返回指定类型的注解
public class MethodTest07 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method14 = Bean.class.getDeclaredMethod("fun", short.class, short.class, Object.class);
    
        System.out.println("\n====1-1 getAnnotations====");
        Annotation[] as1 = method14.getAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====1-2 getAnnotation====");
        Annotation annotation = method14.getAnnotation(注解_METHOD.class);
        System.out.println(annotation);
    
        System.out.println("\n====1-3 getAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = method14.getAnnotationsByType(可重复注解.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
