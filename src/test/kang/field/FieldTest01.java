package test.kang.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// 常规字段测试
public class FieldTest01 {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Bean bean = new Bean();
    
        Field fa = Bean.class.getDeclaredField("a");
        fa.setInt(bean, 1);
        System.out.println(fa.getInt(bean));
    
        Field fb = Bean.class.getDeclaredField("b");
        fb.setInt(bean, 2);
        System.out.println(fb.getInt(bean));
    
        Field fc = Bean.class.getDeclaredField("c");
        fc.setInt(bean, 3);
        System.out.println(fc.getInt(bean));
        
        Field fd = Bean.class.getDeclaredField("d");
        fd.setAccessible(true);    // 需要禁用安全检查
        fd.setInt(bean, 4);
        System.out.println(fd.getInt(bean));
        
        // 获取引用类型的字段
        Field fe = Bean.class.getDeclaredField("e");
        fe.set(bean, "Hello");
        System.out.println(fe.get(bean));

        System.out.println("\n====字段修饰符测试====");
        System.out.println("1："+ Modifier.toString(fa.getModifiers()));
        System.out.println("2："+Modifier.toString(fb.getModifiers()));
        System.out.println("3："+Modifier.toString(fc.getModifiers()));
        System.out.println("4："+Modifier.toString(fd.getModifiers()));
    }
}
