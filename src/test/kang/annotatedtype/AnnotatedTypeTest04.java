package test.kang.annotatedtype;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Field;

// 测试AnnotatedType的实现类(4)：AnnotatedWildcardTypeImpl
public class AnnotatedTypeTest04 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field field34 = Child.class.getField("field4");
        
        AnnotatedType annotatedType = field34.getAnnotatedType();
        System.out.print("annotatedType真实类型：");
        System.out.println(annotatedType.getClass().getSimpleName());
        
        AnnotatedParameterizedType apt = (AnnotatedParameterizedType)annotatedType;
        AnnotatedType[] ats = apt.getAnnotatedActualTypeArguments();
    
        AnnotatedWildcardType awt1 = (AnnotatedWildcardType)ats[0];
        AnnotatedWildcardType awt2 = (AnnotatedWildcardType)ats[1];
        
        
        System.out.println("==========第一个被注解的通配符 aw1==========");
        System.out.println(awt1.getClass().getSimpleName());
    
        AnnotatedType[] upperBounds = awt1.getAnnotatedUpperBounds();
        
        System.out.print("通配符中被注解的上界：");
        System.out.println(upperBounds[0].getClass().getSimpleName());
    
        System.out.print("被注解的上界的注解：");
        System.out.println(upperBounds[0].getAnnotations()[0]);
    
        System.out.print("被注解的上界的类型：");
        System.out.println(upperBounds[0].getType());
        
        System.out.println("==========第二个被注解的通配符 aw2==========");
        System.out.println(awt2.getClass().getSimpleName());
    
        AnnotatedType[] lowerBounds = awt2.getAnnotatedLowerBounds();
    
        System.out.print("通配符中被注解的下界：");
        System.out.println(lowerBounds[0].getClass().getSimpleName());
    
        System.out.print("被注解的下界的注解：");
        System.out.println(lowerBounds[0].getAnnotations()[0]);
    
        System.out.print("被注解的下界的类型：");
        System.out.println(lowerBounds[0].getType());
    }
}
