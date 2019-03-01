package test.kang.constructor;

import test.kang.constructor.annotation.可重复注解;
import test.kang.constructor.annotation.注解_CONSTRUCTOR;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

// 返回所有注解，或返回指定类型的注解（参见AnnotatedElement）
public class ConstructorTest06 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor9 = Bean.class.getDeclaredConstructor(short.class, short.class, Object.class);
        
        System.out.println("\n====1-1 getAnnotations====");
        Annotation[] as1 = constructor9.getAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====1-2 getAnnotation====");
        Annotation annotation = constructor9.getAnnotation(注解_CONSTRUCTOR.class);
        System.out.println(annotation);
    
        System.out.println("\n====1-3 getAnnotationsByType====");
        Annotation[] as2 = constructor9.getAnnotationsByType(可重复注解.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
