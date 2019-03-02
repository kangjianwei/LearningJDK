package test.kang.constructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;

// 获取Receiver Type上的【被注解类型】
public class ConstructorTest10 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean.Test> constructor10 = Bean.Test.class.getDeclaredConstructor(Bean.class);
        
        AnnotatedType at = constructor10.getAnnotatedReceiverType();
        
        System.out.println("Receiver Type：");
        System.out.println(at.getType());
    
        System.out.println();
    
        System.out.println("关联的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
