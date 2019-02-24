package test.kang.clazz.test08;

import java.lang.annotation.Annotation;
import test.kang.clazz.test08.模板02.Child;
import test.kang.clazz.test08.模板02.Parent;
import test.kang.clazz.test08.模板02.可重复注解_不可继承;
import test.kang.clazz.test08.模板02.可重复注解_可继承;

/* 使用模板02 */

// 获取该类上指定类型的注解（不包括继承来的注解）[支持获取@Repeatable类型的注解]
public class ClassTest25 {
    public static void main(String[] args) {
        System.out.println("====getDeclaredAnnotationsByType====");
    
        System.out.println("\n====父类中指定类型的注解（不包括继承而来的注解）====");
        Annotation[] as1 = Parent.class.getDeclaredAnnotationsByType(可重复注解_不可继承.class);
        for (Annotation a : as1){
            System.out.println(a);
        }
        
        
        System.out.println("\n====子类中指定类型的注解（不包括继承而来的注解）====");
        Annotation[] as2 = Child.class.getDeclaredAnnotationsByType(可重复注解_不可继承.class);
        for (Annotation a : as2){
            System.out.println(a);
        }
        
        System.out.println();
        
        Annotation[] as3 = Child.class.getDeclaredAnnotationsByType(可重复注解_可继承.class);
        for (Annotation a : as3){
            System.out.println(a);  // 无输出，因为没有获取到相应的注解
        }
    }
}
