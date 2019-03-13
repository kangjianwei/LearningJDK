package test.kang.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

// 获取形参类型处的【被注解类型】
public class ParameterTest04 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method = Bean.class.getDeclaredMethod("fun", int.class);
    
        Parameter[] parameters = method.getParameters();
    
        AnnotatedType at = parameters[0].getAnnotatedType();
    
        System.out.println("形参类型：");
        System.out.println(at.getType());
    
        System.out.println();
    
        System.out.println("【被注解类型】上的注解：");
        for(Annotation a : at.getAnnotations()){
            System.out.println(a);
        }
    }
}
