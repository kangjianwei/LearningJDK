package test.kang.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

// 抛异常的方法
public class MethodTest06 {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method13 = Bean.class.getDeclaredMethod("fun", long.class);
    
        System.out.println(method13.getName());
        System.out.println(method13.toString());
        System.out.println(method13.toGenericString());
    
        System.out.println("\n====异常类型[类型擦除]====");
        Class[] classes = method13.getExceptionTypes();
        for(Class c : classes){
            System.out.println(c);
        }
    
        System.out.println("\n====异常类型[支持泛型语义]====");
        Type[] types = method13.getGenericExceptionTypes();
        for(Type t : types){
            System.out.println(t);
        }
    
        System.out.println("\n====异常上的【被注解类型】====");
        AnnotatedType[] ats = method13.getAnnotatedExceptionTypes();
        for(AnnotatedType at : ats){
            System.out.println(at.getType());
            for(Annotation a : at.getAnnotations()){
                System.out.println(a);
            }
            System.out.println();
        }
    }
}
