package test.kang.externalizable.test01;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

// 实现序列化接口
public class User01 implements Externalizable {
    private String name;
    private int age;
    
    // 使用Externalizable序列化接口时必须有无参构造方法
    public User01() {
    }
    
    public User01(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // 实现序列化逻辑
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(age);
    }
    
    // 实现反序列化逻辑
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.name = in.readUTF();
        this.age = in.readInt();
    }
    
    @Override
    public String toString() {
        return "User01{" + "name='" + name + '\'' + ", age=" + age + '}';
    }
}
