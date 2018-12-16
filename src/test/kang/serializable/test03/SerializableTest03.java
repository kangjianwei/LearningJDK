package test.kang.serializable.test03;

import java.io.File;
import test.kang.serializable.SerializableUtil;

// 使用transient禁止字段序列化
public class SerializableTest03 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User03 user = new User03("张三", 20);
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test03.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User03 u = (User03)SerializableUtil.deserializableUser(file);
        // 由于禁止了age对象的序列化，因此此处反序列化的结果中，age=0，即没有获取到有效值，使用了int的默认值
        System.out.println(u);
    }
}
