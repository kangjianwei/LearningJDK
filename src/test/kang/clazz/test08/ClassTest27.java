package test.kang.clazz.test08;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import test.kang.clazz.test08.模板03.Child;

/* 使用模板03 */

// 获取该类的被注解父接口
public class ClassTest27 {
    public static void main(String[] args) {
        System.out.println("====getAnnotatedInterfaces（要求注解支持ElementType.TYPE_USE）====");
    
        // 这里的ats代表“@注解01 Interface01, @注解02 Interface02”这个整体
        AnnotatedType[] ats = Child.class.getAnnotatedInterfaces();
        
        System.out.println("\n====获取Annotation====");
        for(AnnotatedType at : ats){
            Annotation[] annotations =at.getAnnotations();
            for (Annotation a : annotations){
                System.out.println(a);
            }
        }
        
        System.out.println("\n====获取Type====");
        for(AnnotatedType at : ats){
            Type type = at.getType();
            System.out.println(type);
        }
    }
}
