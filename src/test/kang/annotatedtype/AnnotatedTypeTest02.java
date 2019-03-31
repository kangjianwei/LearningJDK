package test.kang.annotatedtype;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.Field;

// 测试AnnotatedType的实现类(2)：AnnotatedTypeVariableImpl
public class AnnotatedTypeTest02 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field field2 = Child.class.getField("field2");
        
        AnnotatedType annotatedType = field2.getAnnotatedType();
        System.out.print("annotatedType真实类型：");
        System.out.println(annotatedType.getClass().getSimpleName());
        
        
        AnnotatedTypeVariable atv = (AnnotatedTypeVariable)annotatedType;
        
        System.out.println("==========atv自身==========");
        System.out.print("atv真实类型：");
        System.out.println(atv.getClass().getSimpleName());
        
        System.out.println("atv中的类型注解：目前无法获取");
    
        System.out.print("atv中的类型：");
        System.out.println(atv.getType());
    
        System.out.println("==========atv的上界==========");
        System.out.print("atv的被注解的上界类型：");
        System.out.println(atv.getAnnotatedBounds()[0].getClass().getSimpleName());
    
        System.out.print("atv的被注解的上界类型中的注解：");
        System.out.println(atv.getAnnotatedBounds()[0].getAnnotations()[0]);
    
        System.out.print("atv的被注解的上界类型中的类型：");
        System.out.println(atv.getAnnotatedBounds()[0].getType());
    }
}
