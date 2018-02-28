package test.kang.byteorder;

import java.nio.ByteOrder;

// 判断当前系统存储数据是用大端法还是小端法
public class ByteOrderTest01 {
    public static void main(final String[] arguments) {
        ByteOrder byteOrder = ByteOrder.nativeOrder();
        
        if(byteOrder == ByteOrder.BIG_ENDIAN) {
            System.out.println("大端法");
        } else if(byteOrder == ByteOrder.LITTLE_ENDIAN) {
            System.out.println("小端法");
        } else {
            System.out.println("错误");
        }
    }
}
