package test.kang.serializable.test02;

import java.io.File;
import test.kang.serializable.SerializableUtil;

/*
 * 测试serialVersionUID的使用
 *
 * serialVersionUID可以保证序列化数据的唯一性和兼容性
 *
 * 测试方式：
 *
 * 1. 正常运行程序，序列化和反序列化成功
 * 2. 注释掉SerializableTest02第34行，即直接反序列化之前的对象，依然可以正常运行
 * 3. 在User02中添加一个id属性，即去掉User02中第11行的注释，再次直接反序列化，运行失败
 * 原因是为User02添加了一个属性后，User02的类结构也随之改变，不兼容之前的user.dat了
 *
 * 4. 为User02添加序列化serialVersionUID，即去掉User02第9行注释，保留第11行的注释
 *    去掉SerializableTest02中第34行的注释，重新执行序列化和反序列化，运行成功
 * 5. 注释掉SerializableTest02第34行，即直接反序列化之前的对象，依然可以正常运行
 * 6. 在User02中添加一个id属性，即去掉User02中第11行的注释，再次直接反序列化，依然运行成功
 *
 * 以上描述的是添加属性的情形。
 * 反过来，如果预先设置了serialVersionUID，那么在User02中反序列化之前删掉一个属性，也可以正常运行。
 *
 * 可见，serialVersionUID是class文件的一个身份标记。
 * 虽然类结构可能会发生改变，但只要这个标记一致，那么反序列化依然可以正常执行。
 */
public class SerializableTest02 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User02 user = new User02("张三", 20);
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test02.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
        // 从本地反序列化user对象
        User02 u = (User02)SerializableUtil.deserializableUser(file);
        System.out.println(u);
    }
}
