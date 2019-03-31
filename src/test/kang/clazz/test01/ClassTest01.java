package test.kang.clazz.test01;

// 三种加载类的方式
public class ClassTest01 {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // 方法一
        Class clazz1 = Class.forName("test.kang.clazz.test01.MyClass");
        System.out.println(clazz1.newInstance());
    
        System.out.println("-----------------------------------\n");
        
        // 方法二，第二个参数影响加载的类是否在加载的同时进行初始化
        Class clazz2 = Class.forName("test.kang.clazz.test01.MyClass", false, ClassLoader.getSystemClassLoader());
        System.out.println(clazz2.newInstance());
    
        System.out.println("-----------------------------------\n");
    
        // 方法三，需要先获取所在的Module（这里没有设置Module，所以获取到的是未命名module）
//        Class clazz3 = Class.forName(MyClass.class.getModule(), "test.kang.clazz.test01.MyClass");
//        System.out.println(ClassTest01.class.getModule()+"    "+clazz3.newInstance());
    }
}
