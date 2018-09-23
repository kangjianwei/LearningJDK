package test.kang.stream;

import java.util.Arrays;
import java.util.stream.Stream;

// Stream的简单使用
public class StreamTest01 {
    public static void main(String[] args) {
        Object[] objects = Stream.of("aaa", "bbb", "ccc").toArray();
        System.out.println(Arrays.toString(objects));
        
        Integer[] integers = Stream.of(123, 456, 789).toArray(Integer[]::new);
        System.out.println(Arrays.toString(integers));
    }
}
