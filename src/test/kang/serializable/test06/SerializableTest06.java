package test.kang.serializable.test06;

import java.io.File;
import test.kang.serializable.SerializableUtil;

// 在待序列化的类中自定义序列化/反序列化逻辑
public class SerializableTest06 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User06 user = new User06("张三", 20, new char[]{'男'});
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test06.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User06 u = (User06)SerializableUtil.deserializableUser(file);
        System.out.println(u);
    }
}
