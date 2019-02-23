package test.kang.clazz.test08;

import java.lang.annotation.Annotation;
import test.kang.clazz.test08.模板01.Child;
import test.kang.clazz.test08.模板01.Parent;
import test.kang.clazz.test08.模板01.注解_不可继承_public;
import test.kang.clazz.test08.模板01.注解_可继承_public;

/* 使用模板01 */

// 获取该类上指定类型的注解（包括继承来的注解）
public class ClassTest21 {
    public static void main(String[] args) {
        System.out.println("====getAnnotation====");
        
        System.out.println("\n====父类中指定类型的注解（包括继承来的注解）====");
        Annotation a1 = Parent.class.getAnnotation(注解_不可继承_public.class);
        System.out.println(a1);
        
        System.out.println("\n====子类中指定类型的注解（包括继承来的注解）====");
        Annotation a2 = Child.class.getAnnotation(注解_不可继承_public.class);
        Annotation a3 = Child.class.getAnnotation(注解_可继承_public.class);
        System.out.println(a2);
        System.out.println(a3);
    }
}
