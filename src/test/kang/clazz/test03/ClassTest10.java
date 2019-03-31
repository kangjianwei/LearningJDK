package test.kang.clazz.test03;

import test.kang.clazz.test03.模板.实例类;

// 从外部类获取内部类信息，与getDeclaredClasses()的区别是返回的数组首元素是当前类本身
public class ClassTest10 {
    public static void main(String[] args) {
        // 测试getNestMembers()
        Class[] classes1 = 实例类.class.getNestMembers();
        for(Class c : classes1){
            System.out.println(c.getSimpleName());
        }
    }
}


