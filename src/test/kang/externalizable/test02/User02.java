package test.kang.externalizable.test02;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

// 实现序列化接口
public class User02 implements Externalizable {
    private String name;
    private int age;
    
    // 单例对象
    private static final User02 user = new User02();
    
    // 使用Externalizable序列化接口时必须有无参构造方法
    public User02() {
    }
    
    public User02(String name, int age) {
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
    
    // 如果实现此方法，每次返回的都是相同的user对象
    private Object readResolve(){
        user.name = this.name;
        user.age = this.age;
        return user;
    }
    
    @Override
    public String toString() {
        return "User02{" + "name='" + name + '\'' + ", age=" + age + '}';
    }
}
