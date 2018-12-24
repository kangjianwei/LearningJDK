package test.kang.serializable.test04;

import java.io.File;
import test.kang.serializable.SerializableUtil;

/*
 * 静态字段不会被序列化
 *
 * 测试方式：
 *
 * 1. 执行SerializableTest04，序列化和反序列化均成功
 * 2. 注释掉第27行的代码，即直接反序列化之前保存的对象，也可以运行
 * 3. 去掉第29行的代码，即修改ID为的值为987654321，进行反序列化，发现ID变成了987654321
 *    如果static属性可以序列化，那么此处的ID得到的应该还是123456789才对（反序列化时覆盖了第29行的设置）
 *    但事实上，ID没有被序列化，即ID一直显示为当前设置的值（反序列化时不会覆盖第29行对ID的设置）
 */
public class SerializableTest04 {
    
    public static void main(String[] args) {
        // 将要被序列化的对象
        User04 user = new User04("张三");
        
        // 存储序列化数据的文件
        File file = new File("src/test/kang/serializable/res/test04.dat");
        
        // 将user对象序列化到本地
        SerializableUtil.serializableUser(user, file);
        
//        User04.ID = 987654321;
        
        // 从本地反序列化user对象
        User04 u = (User04) SerializableUtil.deserializableUser(file);
        // ID字段是静态的，属于类，所以不受序列化影响，但会被上面第29行的设置影响
        System.out.println(u);
    }
}
