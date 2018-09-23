package test.kang.stream;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

// min和max测试
public class StreamTest14 {
    public static void main(String[] args) {
        List<Integer> list = List.of(6, 3, 2, 4, 5, 7, 8, 6, 9);
    
        Supplier<Stream<Integer>> supplier = list::stream;
        
        // 按自然顺序求最小值
        System.out.println(supplier.get().min((a, b)->a-b).get());
        
        // 按自然顺序求最大值
        System.out.println(supplier.get().max((a, b)->a-b).get());
    }
}
