package test.kang.clazz.test08;

import java.lang.annotation.Annotation;
import test.kang.clazz.test08.模板01.Child;
import test.kang.clazz.test08.模板01.Parent;

/* 使用模板01 */

// 获取该类上所有注解（不包括继承来的注解）
public class ClassTest22 {
    public static void main(String[] args) {
        System.out.println("====getDeclaredAnnotations====");
        
        System.out.println("\n====父类中所有注解（不包括继承来的注解）====");
        Annotation[] as1 = Parent.class.getDeclaredAnnotations();
        for (Annotation a : as1){
            System.out.println(a);
        }
        
        System.out.println("\n====子类中所有注解（不包括继承来的注解）====");
        Annotation[] as2 = Child.class.getDeclaredAnnotations();
        for (Annotation a : as2){
            System.out.println(a);
        }
    }
}
