package test.kang.externalizable.test02;

import java.io.File;
import test.kang.externalizable.SerializableUtil;

// Externalizable维持单例模式
public class ExternalizableTest02 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User02 user = new User02("张三", 20);
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/externalizable/res/test2.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User02 u1 = (User02) SerializableUtil.deserializableUser(file);
        User02 u2 = (User02) SerializableUtil.deserializableUser(file);
        
        System.out.println("u1=" + u1);
        System.out.println("u2=" + u2);
        
        // 此处应该输出fasle，每次反序列化处的对象都不一样
        System.out.println("u1==u2? " + (u1 == u2));
    }
}
