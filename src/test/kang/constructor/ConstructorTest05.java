package test.kang.constructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

// 抛异常的构造器
public class ConstructorTest05 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor8 = Bean.class.getDeclaredConstructor(long.class);
    
        System.out.println(constructor8.getName());
        System.out.println(constructor8.toString());
        System.out.println(constructor8.toGenericString());
    
        System.out.println("\n====异常类型[类型擦除]====");
        Class[] classes = constructor8.getExceptionTypes();
        for(Class c : classes){
            System.out.println(c);
        }
    
        System.out.println("\n====异常类型[支持泛型语义]====");
        Type[] types = constructor8.getGenericExceptionTypes();
        for(Type t : types){
            System.out.println(t);
        }
        
        System.out.println("\n====异常上的【被注解类型】====");
        AnnotatedType[] ats = constructor8.getAnnotatedExceptionTypes();
        for(AnnotatedType at : ats){
            System.out.println(at.getType());
            for(Annotation a : at.getAnnotations()){
                System.out.println(a);
            }
            System.out.println();
        }
    }
}
