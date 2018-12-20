package test.kang.serializable.test07;

import java.io.File;
import test.kang.serializable.SerializableUtil;

// 维持反序列化对象的单例模式
public class SerializableTest07 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User07 user = new User07("张三", 20, new char[]{'男'});
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test07.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User07 u1 = (User07) SerializableUtil.deserializableUser(file);
        User07 u2 = (User07) SerializableUtil.deserializableUser(file);
        
        System.out.println("u1=" + u1);
        System.out.println("u2=" + u2);
        
        // 此处应该输出true，因为User07中实现了readResolve方法
        System.out.println("u1==u2? " + (u1 == u2));
    }
}
