package test.kang.stream;

import java.util.Arrays;
import java.util.stream.Stream;

// filter测试
public class StreamTest02 {
    public static void main(String[] args) {
        Integer[] integers = Stream
            .of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20)
            .filter(x->x%2==0)  // 筛选偶数
            .filter(x->x>=5)    // >=5
            .filter(x->x<=15)   // <=15
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(integers));
    }
}
