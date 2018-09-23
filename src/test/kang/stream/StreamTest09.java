package test.kang.stream;

import java.util.Arrays;
import java.util.List;

// skip测试
public class StreamTest09 {
    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 6, 3, 2, 4, 5, 7, 8, 6, 9);
    
        // 跳过前5个元素
        Integer[] is = list.stream()
            .skip(5)
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(is));
    }
}
