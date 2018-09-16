package test.kang.stream;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

// Optional测试
public class StreamTest11 {
    public static void main(String[] args) {
        List<Integer> list = List.of(5, 6, 7, 8, 9);
    
        Supplier<Stream<Integer>> supplier = list::stream;
        
        // 找出第一个数
        supplier.get().findFirst().ifPresentOrElse(x-> System.out.println("存在："+x), ()-> System.out.println("不存在"));
    
        // 找出第一个数，并判断它是否大于5
        supplier.get().findFirst().filter(x->x>5).ifPresentOrElse(x-> System.out.println("存在："+x), ()-> System.out.println("不存在"));
    }
}
