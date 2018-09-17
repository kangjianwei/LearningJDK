package test.kang.stream;

import java.util.stream.Stream;

// forEach测试
public class StreamTest12 {
    public static void main(String[] args) {
        // 依次输出流中元素
        Stream.of("aaa", "bbb", "ccc").forEach(System.out::println);
    
        // 依次输出流中元素（按遭遇顺序输出）
        Stream.of(321, 123, 213).forEach(System.out::println);
    }
}
