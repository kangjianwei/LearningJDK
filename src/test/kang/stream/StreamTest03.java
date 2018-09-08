package test.kang.stream;

import java.util.Arrays;
import java.util.stream.Stream;

// map测试
public class StreamTest03 {
    public static void main(String[] args) {
        Integer[] integers = Stream
            .of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20)
            .filter(x->x%2==1)  // 筛选奇数
            .map(x->x*x)        // 求平方后返回
            .toArray(value -> new Integer[value]);
        System.out.println(Arrays.toString(integers));
    
        int[] lens = Stream
            .of("abc", "abcd", "bcdae", "cbacdf", "ah")
            .filter(s->s.startsWith("a"))   // 筛选出所有以'a'开头的字符串
            .mapToInt(s->s.length())        // 求出各字符串的长度
            .toArray();
        System.out.println(Arrays.toString(lens));
    }
}
