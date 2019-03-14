package test.kang.array;

import java.lang.reflect.Array;

// 创建一个参差数组
public class ArrayTest03 {
    public static void main(String[] args) {
        int rows = 3;
        
        // 一维数组实例
        Object[] objects = (Object[]) Array.newInstance(Object.class, rows);
        // 将一维数组中的每个元素设置成二维数组
        for(int i=0; i<Array.getLength(objects); i++){
            String[] arrs = (String[]) Array.newInstance(String.class, i+1);
            Array.set(objects, i, arrs);
        }
        
        // 设置值
        for(int row = 0; row<Array.getLength(objects); row++) {    // 遍历行
            for(int col = 0; col<Array.getLength(objects[row]); col++) { // 遍历列
                Array.set(objects[row], col, row+"-"+col);
            }
        }
    
        // 获取值
        for(int row = 0; row<Array.getLength(objects); row++) {    // 遍历行
            for(int col = 0; col<Array.getLength(objects[row]); col++) { // 遍历列
                System.out.print(Array.get(objects[row], col)+"  ");
            }
            System.out.println();
        }
    }
}
