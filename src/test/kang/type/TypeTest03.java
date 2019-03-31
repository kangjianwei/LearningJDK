package test.kang.type;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

// WildcardType
public class TypeTest03 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field listc = Bean.class.getDeclaredField("listc");
        Field listd = Bean.class.getDeclaredField("listd");
    
        ParameterizedType pt1 = (ParameterizedType) listc.getGenericType();
        Type[] ts1 = pt1.getActualTypeArguments();
        for(Type t : ts1){
            System.out.println(t);
            Type[] types1 = ((WildcardType)t).getUpperBounds();
            if(types1.length>0){
                System.out.println("上界：");
                for(Type type : types1){
                    System.out.println("  "+type);
                }
            }

            Type[] types2 = ((WildcardType)t).getLowerBounds();
            if(types2.length>0){
                System.out.println("下界：");
                for(Type type : types2){
                    System.out.println("  "+type);
                }
            }
        }

        System.out.println("----------------");
    
        ParameterizedType pt2 = (ParameterizedType) listd.getGenericType();
        Type[] ts2 = pt2.getActualTypeArguments();
        for(Type t : ts2){
            System.out.println(t);
            Type[] types1 = ((WildcardType)t).getUpperBounds();
            if(types1.length>0){
                System.out.println("上界：");
                for(Type type : types1){
                    System.out.println("  "+type);
                }
            }
        
            Type[] types2 = ((WildcardType)t).getLowerBounds();
            if(types2.length>0){
                System.out.println("下界：");
                for(Type type : types2){
                    System.out.println("  "+type);
                }
            }
        }
    }
}
