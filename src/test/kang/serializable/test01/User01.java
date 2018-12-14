package test.kang.serializable.test01;

import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User01 implements Serializable {
    private String name;
    private int age;
    private char[] sex; // 数组
    
    public User01(String name, int age, char[] sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }
    
    @Override
    public String toString() {
        return "User01{" + "name='" + name + '\'' + ", age=" + age + ", sex='" + String.valueOf(sex) + '\'' + '}';
    }
}
