package test.kang.constructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;

// 获取返回类型处的【被注解类型】
public class ConstructorTest09 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor9 = Bean.class.getDeclaredConstructor(short.class, short.class, Object.class);
        
        AnnotatedType at = constructor9.getAnnotatedReturnType();
        
        System.out.println("构造器的“返回”类型：（被认为是所在的类的声明）");
        System.out.println(at.getType());
        
        System.out.println();
        
        System.out.println("【被注解类型】上的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
