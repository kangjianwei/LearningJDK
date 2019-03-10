package test.kang.field;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;

// 获取字段类型处的【被注解类型】
public class FieldTest06 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field fh = Bean.class.getDeclaredField("h");    // 基本类型
    
        AnnotatedType at = fh.getAnnotatedType();
    
        System.out.println("字段类型：");
        System.out.println(at.getType());
    
        System.out.println();
    
        System.out.println("【被注解类型】上的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
