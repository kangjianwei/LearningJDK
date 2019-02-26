package test.kang.clazz.test09;

import java.io.IOException;
import java.io.InputStream;

// 测试getResourceAsStream，获取资源
public class ClassTest29 {
    public static void main(String[] args) throws IOException {
        ClassTest29 test = new ClassTest29();
    
        // 从当前类编译后的class文件所在的目录开始搜索资源
        InputStream inputStream = test.getClass().getResourceAsStream("../res/date.txt");
        
        byte[] bytes = new byte[1024];          // 创建缓冲区以接收数据
        inputStream.read(bytes);                // 从date.txt读取输入
        System.out.println(new String(bytes));
    }
}
