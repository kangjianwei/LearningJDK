package test.kang.serializable.test04;

import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User04 implements Serializable {
    public static int ID = 123456789;  // 静态属性属于类，不属于具体的某个对象，不会被序列化
    private String name;
    
    public User04(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "User04{ID=" + ID + " name='" + name + '\'' + '}';
    }
}
