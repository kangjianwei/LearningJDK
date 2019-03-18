package test.kang.annotatedtype;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;

// 测试AnnotatedType的实现类(1)：AnnotatedTypeBaseImpl
public class AnnotatedTypeTest01 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field field1 = Child.class.getField("field1");
    
        AnnotatedType annotatedType = field1.getAnnotatedType();
        System.out.print("annotatedType真实类型：");
        System.out.println(annotatedType.getClass().getSimpleName());
        
        
        AnnotatedType owner = annotatedType.getAnnotatedOwnerType();
    
        System.out.println("==========owner自身==========");
        System.out.print("owner真实类型：");
        System.out.println(owner.getClass().getSimpleName());
        
        System.out.print("owner中的类型注解：");
        System.out.println(owner.getAnnotations()[0]);
        
        System.out.print("owner中的外部类型：");
        System.out.println(owner.getType());
    }
}
