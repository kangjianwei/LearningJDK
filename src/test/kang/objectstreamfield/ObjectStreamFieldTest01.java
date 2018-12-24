package test.kang.objectstreamfield;

import java.io.File;

// 对ObjectStreamField封装的字段进行序列化和反序列化
public class ObjectStreamFieldTest01 {
    public static void main(String[] args) {
        // 将要被序列化的对象
        User user = new User("张三", 20, 100);
    
        // 存储序列化数据的文件
        File file = new File("src/test/kang/objectstreamfield/res/test1.dat");
    
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
    
        // 从本地反序列化user对象
        User u = (User) SerializableUtil.deserializableUser(file);
        System.out.println(u);
    }
}
