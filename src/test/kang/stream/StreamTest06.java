package test.kang.stream;

import java.util.Arrays;
import java.util.List;

// distinct测试
public class StreamTest06 {
    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 6, 3, 2, 4, 5, 7, 8, 6, 9);
        
        // 先去重，再过滤出偶数
        Integer[] is = list.stream()
            .distinct()
            .filter(x->x%2==0)
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(is));
    }
}
