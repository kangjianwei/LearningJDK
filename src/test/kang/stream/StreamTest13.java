package test.kang.stream;

import java.util.Optional;
import java.util.stream.Stream;

// reduce测试
public class StreamTest13 {
    public static void main(String[] args) {
        // 找出最长的单词
        Stream<String> stream1 = Stream.of("I", "am", "Chinese", "!");
        Optional<String> longest = stream1.reduce((s1, s2) -> s1.length() >= s2.length() ? s1 : s2);
        System.out.println(longest.get());
    
        // 计算阶乘
        Stream<Integer> stream2 = Stream.of(1,2,3,4,5);
        Integer factorial = stream2.reduce(
            1,  // (1) 初始值
            (a, b)->(a*b)
        );
        System.out.println(factorial);
        
        // 求单词长度之和
        Stream<String> stream3 = Stream.of("I", "am", "Chinese", "!");
        Integer lengthSum = stream3.reduce(
            0,  // (1) 初始值
            (sum, str) -> sum + str.length(),   // (2) 累加器
            (a, b) -> a + b // (3) 部分和拼接器，并行执行时才会用到
        );
        System.out.println(lengthSum);
    
        // 求和
        Stream<Integer> stream4 = Stream.of(1,2,3,4,5);
        Integer sum = stream4.reduce(
            0,  // (1) 初始值
            (a, b)->(a+b)
        );
        System.out.println(sum);
    
        // 计数
        Stream<Integer> stream5 = Stream.of(1,2,3,4,5);
        Integer count = stream5.reduce(
            0,  // (1) 初始值
            (a, b)->(a+1)
        );
        System.out.println(count);
    
        // 最大值
        Stream<Integer> stream6 = Stream.of(1,6,4,2,3,5,9,7,0,8);
        Integer max = stream6.reduce(
            0,  // (1) 初始值
            (a, b)-> (a>b?a:b)
        );
        System.out.println(max);
    }
}
