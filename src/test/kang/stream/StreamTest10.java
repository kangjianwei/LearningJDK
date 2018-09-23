package test.kang.stream;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

// Stream短路操作测试
public class StreamTest10 {
    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 6, 3, 2, 4, 5, 7, 8, 6, 9);
        
        /*
         * 下面要连续使用流，但一个流只能使用一次
         * 所以，这里借助该函数式接口每次生成一个新的流
         */
        Supplier<Stream<Integer>> supplier = ()->list.stream();
        
        // 存在元素大于5？
        System.out.println(supplier.get().anyMatch(x->x>5));
    
        // 所有元素小于10？
        System.out.println(supplier.get().allMatch(x->x<10));
    
        // 没有元素小于0？
        System.out.println(supplier.get().noneMatch(x->x<0));
        
        // 找出序列中第一个数
        System.out.println(supplier.get().findFirst().get());
    
        // 找到一个元素就返回
        System.out.println(supplier.get().findAny().get());
    }
}
