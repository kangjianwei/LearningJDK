package test.kang.field;

import test.kang.field.annotation.可重复注解;
import test.kang.field.annotation.注解_FIELD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

// 返回所有注解，或返回指定类型的注解
public class FieldTest05 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field fh = Bean.class.getDeclaredField("h");    // 基本类型
    
        System.out.println("\n====2-1 getDeclaredAnnotations====");
        Annotation[] as1 = fh.getDeclaredAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====2-2 getDeclaredAnnotation====");
        Annotation annotation = fh.getDeclaredAnnotation(注解_FIELD.class);
        System.out.println(annotation);
    
        System.out.println("\n====2-3 getDeclaredAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = fh.getDeclaredAnnotationsByType(可重复注解.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
