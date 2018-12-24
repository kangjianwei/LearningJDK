package test.kang.objectstreamfield;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

// 筛选出参与序列化的字段
public class User implements Serializable {
    private String name;
    private int age;
    
    private double weight;  // 该字段不参与序列化
    
    // 指定参与序列化的字段
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("name", String.class),
        new ObjectStreamField("age", int.class)
    };
    
    public User(String name, int age, double weight) {
        this.name = name;
        this.age = age;
        this.weight = weight;
    }
    
    @Override
    public String toString() {
        return "User{" + "name='" + name + '\'' + ", age=" + age + ", weight=" + weight + '}';
    }
    
    /* 自定义序列化/反序列化的流程。如果没有自定义的流程，则按默认方式序列化指定的字段 */
    
    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField fields = s.putFields();
        
        fields.put("name", name);
        fields.put("age", age);
        
        s.writeFields();
    }
    
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        
        // 获取字段时可以关联默认值
        name = (String) fields.get("name", "无名");
        age = fields.get("age", -1);
    }
}
