package test.kang.clazz.test09;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

// 测试getResource()，获取资源
public class ClassTest28 {
    public static void main(String[] args) throws URISyntaxException, IOException {
        ClassTest28 test = new ClassTest28();
        
        // 从当前类编译后的class文件所在的目录开始搜索资源
        System.out.println("资源路径："+test.getClass().getResource("../res/date.txt"));
        
        URL url = test.getClass().getResource("../res/date.txt");
        URI uri = url.toURI();
        File file = new File(uri);
        
        System.setIn(new FileInputStream(file));    // 设置输入源
    
        byte[] bytes = new byte[1024];          // 创建缓冲区以接收数据
        System.in.read(bytes);                  // 从date.txt读取输入
        System.out.println(new String(bytes));
    }
}
