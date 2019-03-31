package test.kang.annotatedtype;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;

// 测试AnnotatedType的实现类(5)：AnnotatedArrayTypeImpl
public class AnnotatedTypeTest05 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field field5 = Child.class.getField("field5");
    
        AnnotatedType annotatedType = field5.getAnnotatedType();
        System.out.print("annotatedType真实类型：");
        System.out.println(annotatedType.getClass().getSimpleName());
    
        AnnotatedArrayType aat = (AnnotatedArrayType)annotatedType;
        
        System.out.print("aat真实类型：");
        System.out.println(aat.getClass().getSimpleName());
    
        System.out.print("aat中的被注解通用组件类型：");
        System.out.println(aat.getAnnotatedGenericComponentType().getClass().getSimpleName());
    
        System.out.print("aat中的被注解通用组件类型的注解：");
        System.out.println(aat.getAnnotatedGenericComponentType().getAnnotations()[0]);
        
        System.out.print("aat中的被注解通用组件类型的类型：");
        System.out.println(aat.getAnnotatedGenericComponentType().getType());
    }
}
