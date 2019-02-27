package test.kang.constructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.TypeVariable;

// 返回构造器引入的TypeVariable
public class ConstructorTest02 {
    public static void main(String[] args) throws NoSuchMethodException {
        Constructor<Bean> constructor5 = Bean.class.getDeclaredConstructor(double.class);
        
        System.out.println(constructor5.getName());
        System.out.println(constructor5.toString());
        System.out.println(constructor5.toGenericString());
        
        System.out.println("\n====输出TypeVariable====");
        TypeVariable<Constructor<Bean>>[] typeVariables = constructor5.getTypeParameters();
        for(TypeVariable<Constructor<Bean>> typeVariable : typeVariables){
            System.out.println(typeVariable);
        }
    }
}
