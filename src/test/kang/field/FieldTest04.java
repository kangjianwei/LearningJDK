package test.kang.field;

import test.kang.field.annotation.可重复注解;
import test.kang.field.annotation.注解_FIELD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

// 返回所有注解，或返回指定类型的注解
public class FieldTest04 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field fh = Bean.class.getDeclaredField("h");    // 基本类型
    
        System.out.println("\n====1-1 getAnnotations====");
        Annotation[] as1 = fh.getAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====1-2 getAnnotation====");
        Annotation annotation = fh.getAnnotation(注解_FIELD.class);
        System.out.println(annotation);
    
        System.out.println("\n====1-3 getAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = fh.getAnnotationsByType(可重复注解.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
