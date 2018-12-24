package test.kang.serializable.test01;

import java.io.File;
import test.kang.serializable.SerializableUtil;

// 序列化/反序列化【对象】的属性
public class SerializableTest01 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User01 user = new User01("张三", 20, new char[]{'男'});
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test01.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User01 u = (User01) SerializableUtil.deserializableUser(file);
        System.out.println(u);
    }
}
