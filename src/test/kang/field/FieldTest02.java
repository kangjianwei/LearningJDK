package test.kang.field;

import java.lang.reflect.Field;

// 字段类型测试
public class FieldTest02 {
    public static void main(String[] args) throws NoSuchFieldException {
        Field fa = Bean.class.getDeclaredField("a");    // 基本类型
        Field fe = Bean.class.getDeclaredField("e");    // 引用类型
        Field ff = Bean.class.getDeclaredField("f");    // 泛型
        Field fg = Bean.class.getDeclaredField("g");    // 泛型
        
        System.out.println("\n====获取字段类型[类型擦除]====");
        System.out.println(fa.getType());
        System.out.println(fe.getType());
        System.out.println(ff.getType());
        System.out.println(fg.getType());
    
        System.out.println("\n====获取字段类型[支持泛型语义]====");
        System.out.println(fa.getGenericType());
        System.out.println(fe.getGenericType());
        System.out.println(ff.getGenericType());
        System.out.println(fg.getGenericType());
    }
}
