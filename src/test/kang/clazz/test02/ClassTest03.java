package test.kang.clazz.test02;

// 获取引用类型名称
public class ClassTest03 {
    public static void main(String[] args){
        System.out.println("\n=================普通类=================");
        System.out.println(ClassTest03.class.getName());
        System.out.println(ClassTest03.class.getSimpleName());
        System.out.println(ClassTest03.class.getCanonicalName());
        System.out.println(ClassTest03.class.getTypeName());
    
        System.out.println("\n=================内部类=================");
        System.out.println(内部类.class.getName());
        System.out.println(内部类.class.getSimpleName());
        System.out.println(内部类.class.getCanonicalName());
        System.out.println(内部类.class.getTypeName());
        
        System.out.println("\n=================匿名类=================");
        接口 接口对象 = new 接口() {
            @Override
            public void run() {
                System.out.println(this.getClass().getName());
                System.out.println(this.getClass().getSimpleName());
                System.out.println(this.getClass().getCanonicalName());
                System.out.println(this.getClass().getTypeName());
            }
        };
        接口对象.run();
    }
    
    
    class 内部类 {
    }
}

interface 接口{
    void run();
}
