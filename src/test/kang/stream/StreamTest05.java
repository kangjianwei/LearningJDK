package test.kang.stream;

import java.util.Arrays;
import java.util.stream.Stream;

// peek测试
public class StreamTest05 {
    public static void main(String[] args) {
        int[] i1 = new int[]{1, 2, 3};
        int[] i2 = new int[]{4, 5, 6, 7};
        int[] i3 = new int[]{8, 9};
        
        int[][] ii = new int[][]{i1, i2, i3};
        
        // 加入peek可以查看中间操作
        int[] is = Stream.of(ii)
            .peek(x-> System.out.println(Arrays.toString(x)+" "))
            .flatMapToInt(x-> Arrays.stream(x))
            .peek(x-> System.out.println(x+" "))
            .toArray();
        
        System.out.println("最后输出："+ Arrays.toString(is));
    }
}
