package test.kang.type;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

// TypeVariable
public class TypeTest01 {
    public static void main(String[] args) {
        TypeVariable[] tvs = Bean.class.getTypeParameters();
        
        for(TypeVariable tv : tvs){
            System.out.println(tv.getName());
            System.out.println(tv.getGenericDeclaration());
            Type[] types =tv.getBounds();
            for(Type t : types){
                System.out.println(t);
            }
            System.out.println("----------------");
        }
    }
}
