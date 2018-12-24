package test.kang.random;

import java.util.Random;

// 自定义随机数生成器的种子
public class RandomTest02 {
    public static void main(String[] args) {
        Random random1= new Random(12345);
    
        System.out.println("该随机数生成器使用固定的种子，所以每次输出的随机数序列是相同的");
        for(int i=0; i<10; i++){
            System.out.print(random1.nextInt()+"  ");
        }
        
        // 这次使用系统时间作随机数生成器的种子
        Random random2= new Random(System.nanoTime());
    
        System.out.println("\n\n该随机数生成器使用随机种子，所以每次输出的随机数序列很难相同");
        for(int i=0; i<10; i++){
            System.out.print(random2.nextInt()+"  ");
        }
    }
}
