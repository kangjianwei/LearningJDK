package test.kang.array;

import java.lang.reflect.Array;

// 创建一维数组
public class ArrayTest01 {
    public static void main(String[] args) {
        int length = 10;
        
        // 创建实例
        int[] arrays = (int[]) Array.newInstance(int.class, length);
        
        System.out.println("数组长度："+ Array.getLength(arrays));
        
        // 设置值
        for(int i=0; i<length; i++){
            Array.setInt(arrays, i, i*i);
        }
        
        // 获取值
        for(int i=0; i<length; i++){
            System.out.print(Array.getInt(arrays, i)+" ");
        }
    }
}
