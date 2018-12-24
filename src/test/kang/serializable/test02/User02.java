package test.kang.serializable.test02;

import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User02 implements Serializable {
    
    // 这里的序列化ID必须用static final修饰，变量名必须是serialVersionUID
//    private static final long serialVersionUID = 12345L;

//    private int id = 123456789;
    
    private String name;
    private int age;
    
    public User02(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "User02{" + "name='" + name + '\'' + ", age=" + age + '}';
    }
}
