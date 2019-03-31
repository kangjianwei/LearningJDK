package test.kang.field;

import java.lang.reflect.Field;

// 字段名称
public class FieldTest03 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field fa = Bean.class.getDeclaredField("a");    // 基本类型
        Field fe = Bean.class.getDeclaredField("e");    // 引用类型
        Field ff = Bean.class.getDeclaredField("f");    // 泛型
        Field fg = Bean.class.getDeclaredField("g");    // 泛型
        
        System.out.println("\n====基本类型字段====");
        System.out.println(fa.getName());
        System.out.println(fa.toString());
        System.out.println(fa.toGenericString());
    
        System.out.println("\n====引用类型字段====");
        System.out.println(fe.getName());
        System.out.println(fe.toString());
        System.out.println(fe.toGenericString());
    
        System.out.println("\n====类型变量（无显式上界）====");
        System.out.println(ff.getName());
        System.out.println(ff.toString());
        System.out.println(ff.toGenericString());
    
        System.out.println("\n====类型变量（有显式上界）====");
        System.out.println(fg.getName());
        System.out.println(fg.toString());
        System.out.println(fg.toGenericString());
    }
}
