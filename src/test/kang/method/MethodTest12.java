package test.kang.method;

import java.lang.reflect.Method;

// 桥接方法
public class MethodTest12 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method mc = Child.class.getDeclaredMethod("get", Object.class);
        System.out.println(mc.isBridge());
    
        Method mp = Parent.class.getDeclaredMethod("get", Object.class);
        System.out.println(mp.isBridge());
    }
}

class Parent<T> {
    // 在运行中被擦除类型，但在子类中会生成一个相同签名的桥接方法
    public T get(T a) {
        return a;
    }
}

class Child extends Parent<String> {
    @Override
    public String get(String a) {
        return a;
    }
}
