package test.kang.method;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

// 返回方法引入的TypeVariable
public class MethodTest02 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method5 = Bean.class.getDeclaredMethod("fun", double.class);

        System.out.println(method5.getName());
        System.out.println(method5.toString());
        System.out.println(method5.toGenericString());
        
        System.out.println("\n====输出TypeVariable====");
        TypeVariable<Method>[] typeVariables = method5.getTypeParameters();
        for(TypeVariable<Method> typeVariable : typeVariables){
            System.out.println(typeVariable);
        }
    }
}
