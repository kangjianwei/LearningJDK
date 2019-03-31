package test.kang.annotatedtype;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;

// 测试AnnotatedType的实现类(3)：AnnotatedParameterizedTypeImpl
public class AnnotatedTypeTest03 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field field34 = Child.class.getField("field3");
    
        AnnotatedType annotatedType = field34.getAnnotatedType();
        System.out.print("annotatedType真实类型：");
        System.out.println(annotatedType.getClass().getSimpleName());
    
        
        AnnotatedParameterizedType apt = (AnnotatedParameterizedType)annotatedType;
    
        System.out.println("==========apt自身==========");
        System.out.print("apt真实类型：");
        System.out.println(apt.getClass().getSimpleName());
    
        System.out.print("apt中的类型注解：");
        System.out.println(apt.getAnnotations()[0]);
    
        System.out.print("apt中的参数化类型：");
        System.out.println(apt.getType());
    
        System.out.println("==========apt中的被注解的通配符参数==========");
        AnnotatedType[] annotatedTypes = apt.getAnnotatedActualTypeArguments();
        for(AnnotatedType at : annotatedTypes){
            System.out.println(at);
        }
    }
}
