package test.kang.clazz.test01;

public class MyClass {
    static {
        System.out.println("--静态初始化（只执行一次）--");
    }
    
    public MyClass() {
        System.out.println("--构造方法--");
    }
}
