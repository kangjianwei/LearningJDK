package test.kang.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;

// 返回参数上的注解
public class MethodTest09 {
    public static void main(String[] args) throws NoSuchMethodException {
        int count = 0;
        
        Method method14 = Bean.class.getDeclaredMethod("fun", short.class, short.class, Object.class);
    
        System.out.println("\n获取参数上的注解");
        Annotation[][] ass = method14.getParameterAnnotations();
        for(Annotation[] as : ass) {
            System.out.println(">> 第" + (++count) + "个参数上的注解：");
            for(Annotation a : as) {
                System.out.println(a);
            }
            System.out.println();
        }
    
        System.out.println("\n===形参处的【被注解类型】===");
        AnnotatedType[] ats = method14.getAnnotatedParameterTypes();
        for(AnnotatedType at : ats){
            System.out.println(at.getType());
            for(Annotation a : at.getAnnotations()){
                System.out.println(a);
            }
            System.out.println();
        }
    }
}
