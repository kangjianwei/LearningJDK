package test.kang.clazz.test05;

import java.lang.reflect.Field;
import test.kang.clazz.test05.模版.实例类;

// 获取public字段，包括父类/父接口中的public字段
public class ClassTest12 {
    public static void main(String[] args) throws NoSuchFieldException {
    
        // 返回当前类中所有public字段，包括父类/父接口中的public字段
        System.out.println("\n====getFields====");
        Field[] fields = 实例类.class.getFields();
        for (Field field : fields) {
            System.out.println(field.getName());
        }
    
        // 返回当前类中指定名称的public字段，包括父类/父接口中的public字段
        System.out.println("\n====getField====");
        Field field1 = 实例类.class.getField("实例类字段_public");
        System.out.println(field1.getName());
        Field field2 = 实例类.class.getField("抽象类字段_public");
        System.out.println(field2.getName());
        Field field3 = 实例类.class.getField("接口字段_public");
        System.out.println(field3.getName());
    }
}
