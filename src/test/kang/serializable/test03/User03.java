package test.kang.serializable.test03;

import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User03 implements Serializable {
    private String name;
    
    // 使用transient关键字修饰属性后，该属性不会被序列化
    private transient int age;
    
    public User03(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "User03{" + "name='" + name + '\'' + ", age=" + age + '}';
    }
}
