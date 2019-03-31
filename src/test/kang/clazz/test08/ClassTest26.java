package test.kang.clazz.test08;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import test.kang.clazz.test08.模板03.Child;
import test.kang.clazz.test08.模板03.Parent;

/* 使用模板03 */

// 获取该类的被注解父类
public class ClassTest26 {
    public static void main(String[] args) {
        System.out.println("====getAnnotatedSuperclass（要求注解支持ElementType.TYPE_USE）====");
    
        // 这里的at代表“@注解01 @注解02 Parent”这个整体，而不单是Parent
        AnnotatedType at = Child.class.getAnnotatedSuperclass();
        
        System.out.println("\n====获取Annotation====");
        Annotation[] annotations =at.getAnnotations();
        for (Annotation a : annotations){
            System.out.println(a);
        }
        
        System.out.println("\n====获取Type====");
        Type type = at.getType();
        System.out.println(type);
        System.out.println(type== Parent.class);
    }
}
