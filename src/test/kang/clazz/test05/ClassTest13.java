package test.kang.clazz.test05;

import java.lang.reflect.Field;
import test.kang.clazz.test05.模版.实例类;

// 获取所有字段，但不包括父类/父接口中的字段
public class ClassTest13 {
    public static void main(String[] args) throws NoSuchFieldException {
    
        // 返回当前类中所有字段，但不包括父类/父接口中的字段
        System.out.println("\n====getDeclaredFields====");
        Field[] fields = 实例类.class.getDeclaredFields();
        for (Field field : fields) {
            System.out.println(field.getName());
        }
    
        // 返回当前类中指定名称的字段，但不包括父类/父接口中的字段
        System.out.println("\n====getDeclaredField====");
        Field field = 实例类.class.getDeclaredField("实例类字段_private");
        System.out.println(field.getName());
    }
}
