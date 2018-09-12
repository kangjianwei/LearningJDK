package test.kang.stream;

import java.util.Arrays;
import java.util.List;

// sorted测试
public class StreamTest07 {
    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 6, 3, 2, 4, 5, 7, 8, 6, 9);
    
        // 默认排序，按升序排序
        Integer[] is1 = list.stream()
            .sorted()
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(is1));
    
        // 自定义排序（这里进行降序排序）
        Integer[] is2 = list.stream()
            .sorted((o1, o2) -> o2 - o1)
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(is2));
    }
}
