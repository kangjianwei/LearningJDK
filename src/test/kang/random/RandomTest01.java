package test.kang.random;

import java.util.Random;

// 使用默认的Random生成随机数序列
public class RandomTest01 extends Random{
    /*
     * 这里继承Random只是为了测试它的next(int)方法，因为该方法是protected方法
     */
    
    public static void main(String[] args) {
        RandomTest01 random = new RandomTest01();
        
        System.out.print("--------------打印随机数--------------");
        
        System.out.println("\n\n输出10个随机byte值，存入bytes数组，有正有负");
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        System.out.print("[nextBytes(byte[])]  ");
        for(int i=0; i<10; i++){
            System.out.print(bytes[i]+"  ");
        }
        
        System.out.println("\n\n输出10个随机int值，有正有负");
        System.out.print("[nextInt()]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextInt()+"  ");
        }
        
        System.out.println("\n\n输出10个随机int值，值的范围是[0, 20)");
        System.out.print("[nextInt(int)]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextInt(20)+"  ");
        }
    
        System.out.println("\n\n输出10个随机long值，有正有负");
        System.out.print("[nextLong()]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextLong()+"  ");
        }
    
        System.out.println("\n\n输出10个随机float值，值的范围是[0, 1)");
        System.out.print("[nextFloat()]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextFloat()+"  ");
        }
    
        System.out.println("\n\n输出10个随机double值，值的范围是[0, 1)");
        System.out.print("[nextDouble()]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextDouble()+"  ");
        }
    
        System.out.println("\n\n输出10个随机double值，有正有负");
        System.out.print("[nextGaussian()]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.nextGaussian()+"  ");
        }
        
        System.out.println("\n\n输出10个随机int值，值的范围是[0, 16)");
        System.out.print("[next(int)]  ");
        for(int i=0; i<10; i++){
            System.out.print(random.next(4)+"  ");
        }
    
    }
}
