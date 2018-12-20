package test.kang.serializable.test07;

import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User07 implements Serializable {
    private String name;
    private int age;
    private char[] sex; // 数组
    
    public static final User07 user= new User07();
    
    public User07() {
    }
    
    public User07(String name, int age, char[] sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }
    
    // 如果实现此方法，每次返回的都是相同的user对象
    private Object readResolve(){
        user.name = this.name;
        user.age = this.age;
        user.sex = this.sex;
        return user;
    }
    
    @Override
    public String toString() {
        return "User07{" + "name='" + name + '\'' + ", age=" + age + ", sex='" + String.valueOf(sex) + '\'' + '}';
    }
}
