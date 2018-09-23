package test.kang.stream;

import java.util.stream.Stream;

// count测试
public class StreamTest15 {
    public static void main(String[] args) {
        
        // 计数
        System.out.println(Stream.of(6, 3, 2, 4, 5, 7).count());
    }
}
