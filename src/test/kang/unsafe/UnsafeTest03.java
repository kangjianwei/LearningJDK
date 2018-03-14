package test.kang.unsafe;

import sun.misc.Unsafe;

// 本地内存操作
public class UnsafeTest03 {
    private static long GB = 1024 * 1024 * 1024;    // 1GB=1024*1024*1024 byte
    
    public static void main(String[] args) {
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        
        /* 内存的分配与释放 */
        
        long address = unsafe.allocateMemory(GB / 2);   // 申请512MB的本地内存，并返回分配的内存地址
        unsafe.setMemory(address, GB / 2, (byte) 0);     // 将申请的内存块填充为0
        
        // 扩容到1GB，如果扩容成功，原来的address就作废了
        long newAddress = unsafe.reallocateMemory(address, GB);
        unsafe.setMemory(newAddress, GB, (byte) 0);    // 将申请的内存块填充为0
        
        // !!! 用完记得释放内存
        unsafe.freeMemory(newAddress);
        
        
        /* 内存的赋值与拷贝 */
        
        long src = unsafe.allocateMemory(10 * 4);  // 相当于创建长度为10的int数组
        unsafe.setMemory(src, 10 * 4, (byte) 0);
        
        // 为src指向的内存块赋值
        for(int i = 0; i < 10; i++) {
            unsafe.putInt(src + i * 4, i);
        }
        
        // 输出src指向的内存块中的值
        System.out.print("src = ");
        for(int i = 0; i < 10; i++) {
            System.out.print(unsafe.getInt(src + i * 4) + " ");
        }
        System.out.println();
    
        long dst = unsafe.allocateMemory(10 * 4);  // 相当于创建长度为10的int数组
        unsafe.setMemory(dst, 10 * 4, (byte) 0);
        
        unsafe.copyMemory(src, dst, 10*4);
        // 输出dst指向的内存块中的值
        System.out.print("dst = ");
        for(int i = 0; i < 10; i++) {
            System.out.print(unsafe.getInt(dst + i * 4) + " ");
        }
        System.out.println();
    }
}
