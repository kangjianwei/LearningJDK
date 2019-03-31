package test.kang.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import test.kang.parameter.annotation.可重复注解;
import test.kang.parameter.annotation.注解_PARAMETER;

// 返回所有注解，或返回指定类型的注解
public class ParameterTest03 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method = Bean.class.getDeclaredMethod("fun", int.class);
    
        Parameter[] parameters = method.getParameters();
    
        System.out.println("\n====2-1 getDeclaredAnnotations====");
        Annotation[] as1 = parameters[0].getDeclaredAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====2-2 getDeclaredAnnotation====");
        Annotation annotation = parameters[0].getDeclaredAnnotation(注解_PARAMETER.class);
        System.out.println(annotation);
    
        System.out.println("\n====2-3 getDeclaredAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = parameters[0].getDeclaredAnnotationsByType(可重复注解.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
