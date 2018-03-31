package test.kang.unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

// 利用Unsafe#objectFieldOffse方法获取某个对象的近似大小
public class UnsafeTest04 {
    public static void main(String[] args) {
        System.out.println("Entry对象占用的内存大小(字节)：" + sizeOf(Entry.class));
    }
    
    public static long sizeOf(Class<?> clazz) {
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        
        long maxOffset = 0;
        
        do {
            // 遍历指定的类及其父对象，找到偏移量最大的那个字段（内存分布中最后那个字段）
            for(Field f : clazz.getDeclaredFields()) {
                if(!Modifier.isStatic(f.getModifiers())) {
                    maxOffset = Math.max(maxOffset, unsafe.objectFieldOffset(f));
                }
            }
        } while((clazz = clazz.getSuperclass()) != null);
        
        return maxOffset + 8;   // 字段占用内存的最大值为long或者double：8个字节
    }
}
