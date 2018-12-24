package test.kang.serializable.test06;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// 实现Serializable接口的才可以被序列化
public class User06 implements Serializable {
    private String name;
    private transient int age;  // 此处的transient关键字失效，因为使用了自定义的序列化逻辑
    private char[] sex; // 数组
    
    public User06(String name, int age, char[] sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        System.out.println("-----自定义序列化逻辑-----");
        
        oos.writeUTF(name);
        oos.writeInt(age);
        oos.writeObject(sex);
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        System.out.println("-----自定义反序列化逻辑-----");
    
        this.name = ois.readUTF();
        this.age = ois.readInt();
        this.sex = (char[]) ois.readObject();
    }
    
    @Override
    public String toString() {
        return "User07{" + "name='" + name + '\'' + ", age=" + age + ", sex='" + String.valueOf(sex) + '\'' + '}';
    }
}
