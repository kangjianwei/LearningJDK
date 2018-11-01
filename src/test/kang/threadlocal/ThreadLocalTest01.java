package test.kang.threadlocal;

// 测试魔数0x61c88647
public class ThreadLocalTest01 {
    private static final int HASH_INCREMENT = 0x61c88647;
    
    public static void main(String[] args) {
        // 随着s的不同，序列中的元素顺序也会改变
        int s = 0;
        
        double n = 4;
        
        int Max = (int) Math.pow(2, n);
        
        // 如果循环次数大于Max，就会出现重复的值了
        for(int i = s; i<Max + s; i++) {
            System.out.print(((i * HASH_INCREMENT) & (Max - 1)) + " ");
        }
    }
}
