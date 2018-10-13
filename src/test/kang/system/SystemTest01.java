package test.kang.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

// 标准流
public class SystemTest01 {
    public static void main(String[] args) throws IOException {
        byte[] bytes = new byte[1024];          // 创建缓冲区以接收数据
        
        System.out.println("-----------------从控制台输入/输出内容-----------------");
        System.in.read(bytes);                  // 从控制台读取输入
        System.out.println(new String(bytes));  // 输出读取到的内容到控制台
        
        
        System.out.println("-----------------从文件输入/输出内容-----------------");
        System.setIn(new FileInputStream(new File("src/test/kang/system/in.txt")));                      // 改变输入流源头
        System.setOut(new PrintStream(new FileOutputStream(new File("src/test/kang/system/out.txt"))));  // 改变输出流终点
        System.in.read(bytes);                  // 从in.txt读取输入
        System.out.println(Math.random());      // 先输出一个随机数，目的是验证每次都是新的输出
        System.out.println(new String(bytes));  // 输出读取到的内容到out.txt
    }
}
