package test.kang.constructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;

// 返回参数上的注解
public class ConstructorTest08 {
    public static void main(String[] args) throws NoSuchMethodException {
        int count = 0;
        
        Constructor<Bean> constructor9 = Bean.class.getDeclaredConstructor(short.class, short.class, Object.class);
        
        System.out.println("\n获取参数上的注解");
        Annotation[][] ass = constructor9.getParameterAnnotations();
        for(Annotation[] as : ass) {
            System.out.println(">> 第" + (++count) + "个参数上的注解：");
            for(Annotation a : as) {
                System.out.println(a);
            }
            System.out.println();
        }
        
        System.out.println("\n===形参处的【被注解类型】===");
        AnnotatedType[] ats = constructor9.getAnnotatedParameterTypes();
        for(AnnotatedType at : ats){
            System.out.println(at.getType());
            for(Annotation a : at.getAnnotations()){
                System.out.println(a);
            }
            System.out.println();
        }
    }
}
