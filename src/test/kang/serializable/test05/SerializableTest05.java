package test.kang.serializable.test05;

import java.io.File;
import test.kang.serializable.SerializableUtil;

/*
 * 序列化与继承
 *
 * 如果父类没有实现序列化接口，那么序列化子类时会忽略继承的属性。
 *
 * 测试方式：
 *
 * 1. 直接运行，反序列化时子类属性值为123，继承来的属性值为0，显然继承来的属性美欧被序列化
 * 2. 让父类Parent实现Serializable接口，重新运行程序，发现子类属性和继承来的属性都被序列化了
 * 3. 如果父类已经序列化，那么子类会“继承”这种特性，也就是说子类也被视为可以序列化
 */
public class SerializableTest05 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        Child child = new Child(123, 456);
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test05.dat");
        
        // 将child对象序列化到本地
        SerializableUtil.serializableUser(child, file);
        
        // 从本地反序列化user对象
        Child c = (Child) SerializableUtil.deserializableUser(file);
        System.out.println(c);
    }
}
