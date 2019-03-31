package test.kang.type;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;

// GenericArrayType
public class TypeTest04 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field xa = Bean.class.getDeclaredField("xa");
        Field xb = Bean.class.getDeclaredField("xb");
    
        GenericArrayType grt1 = (GenericArrayType) xa.getGenericType();
        System.out.println(grt1);
        System.out.println(grt1.getGenericComponentType());
        
        System.out.println("----------------");
    
        GenericArrayType grt2 = (GenericArrayType) xb.getGenericType();
        System.out.println(grt2);
        System.out.println(grt2.getGenericComponentType());
        System.out.println(((GenericArrayType)grt2.getGenericComponentType()).getGenericComponentType());
    }
}
