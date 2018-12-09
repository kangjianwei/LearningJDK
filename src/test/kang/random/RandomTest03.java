package test.kang.random;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

// Random流
public class RandomTest03 {
    public static void main(String[] args) {
        Random random = new Random(System.nanoTime());
    
        System.out.println("\n->int流");
        
        // 可以生成20个随机int值的流，取值范围是-100, 100
        IntStream intStream = random.ints(20, -100, 100);
        
        int[] arr1 = intStream
            .peek(x -> System.out.println(x + "  " + x % 20)) // 查看内部结构，可验证后续的取余操作确实成功了
            .map(x -> x % 20)  // 对每个随机数取余
            .toArray(); // 收集到数组
        
        // 打印取余后的随机数
        System.out.println(Arrays.toString(arr1));
    
    
        System.out.println("\n->long流");
    
        // 可以生成20个随机long值的流，取值范围是-100, 100
        LongStream longStream = random.longs(20, -100, 100);
        long[] arr3 = longStream.toArray(); // 收集到数组
        System.out.println(Arrays.toString(arr3));  // 打印随机数
        
    
        System.out.println("\n->double流");
    
        // 可以生成20个随机double值的流，取值范围是-100, 100
        DoubleStream doubleStream = random.doubles(20, -100, 100);
        double[] arr2 = doubleStream.toArray(); // 收集到数组
        System.out.println(Arrays.toString(arr2));  // 打印随机数
    }
}
