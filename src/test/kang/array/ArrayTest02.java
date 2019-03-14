package test.kang.array;

import java.lang.reflect.Array;

// 创建二维数组
public class ArrayTest02 {
    public static void main(String[] args) {
        int rows = 3;
        int cols = 4;
        
        // 创建实例
        String[][] arrays = (String[][]) Array.newInstance(String.class, rows, cols);
        
        System.out.println("行数：" + Array.getLength(arrays));
        System.out.println("列数：" + Array.getLength(arrays[0]));
    
        System.out.println();
        
        // 设置值
        for(int row = 0; row<Array.getLength(arrays); row++) {    // 遍历行
            for(int col = 0; col<Array.getLength(arrays[row]); col++) { // 遍历列
                Array.set(arrays[row], col, row+"-"+col);
            }
        }
        
        // 获取值
        for(int row = 0; row<Array.getLength(arrays); row++) {    // 遍历行
            for(int col = 0; col<Array.getLength(arrays[row]); col++) { // 遍历列
                System.out.print(Array.get(arrays[row], col)+"  ");
            }
            System.out.println();
        }
    }
}
