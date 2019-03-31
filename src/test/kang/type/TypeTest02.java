package test.kang.type;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// ParameterizedType
public class TypeTest02 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field lista = Bean.class.getDeclaredField("lista");
        Field listb = Bean.class.getDeclaredField("listb");
        Field entry = Bean.class.getDeclaredField("entry");
        
        ParameterizedType pta = (ParameterizedType) lista.getGenericType();
        System.out.println(pta.getRawType());
        System.out.println(pta.getOwnerType());
        Type[] types1 = pta.getActualTypeArguments();
        for(Type type : types1){
            System.out.println(type);
        }
        
        System.out.println("----------------");
    
        ParameterizedType ptb = (ParameterizedType) listb.getGenericType();
        System.out.println(ptb.getRawType());
        System.out.println(ptb.getOwnerType());
        Type[] types2 = ptb.getActualTypeArguments();
        for(Type type : types2){
            System.out.println(type);
        }
    
        System.out.println("----------------");
    
        ParameterizedType ptc = (ParameterizedType) entry.getGenericType();
        System.out.println(ptc.getRawType());
        System.out.println(ptc.getOwnerType());
        Type[] types3 = ptc.getActualTypeArguments();
        for(Type type : types3){
            System.out.println(type);
        }
    }
}
