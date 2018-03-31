package test.kang.unsafe;

import sun.misc.Unsafe;

// 对JVM内存中某对象的数组字段/变量直接操作
public class UnsafeTest02 {
    static char[] chars = new char[]{'西', '游', '记', '\0', '\0', '\0',};
    
    public static void main(String[] args) throws NoSuchFieldException, InstantiationException {
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        
        long B = unsafe.arrayBaseOffset(char[].class);  // 寻找某类型数组中的元素时约定的起始偏移地址
        long S = unsafe.arrayIndexScale(char[].class);  // 某类型数组每个元素所占字节数
        
        // 输出数组中所有元素
        for(int n=0; n<chars.length; n++){
            char c = unsafe.getChar(chars, B+n*S);
            if(c!='\0'){
                System.out.print(c+" ");
            }
        }
        System.out.println();
        
        // 修改数组元素的值
        unsafe.putChar(chars, B+3*S, '吴');  // chars[3]='吴'
        unsafe.putChar(chars, B+4*S, '承');  // chars[4]='承'
        unsafe.putChar(chars, B+5*S, '恩');  // chars[5]='恩'
        
        // 修改数据后，输出数组中所有元素
        for(int n=0; n<chars.length; n++){
            char c = unsafe.getChar(chars, B+n*S);
            if(c!='\0'){
                System.out.print(c+" ");
            }
        }
        System.out.println();
    }
}
