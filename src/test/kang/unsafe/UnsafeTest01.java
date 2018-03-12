package test.kang.unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import sun.misc.Unsafe;

// 获取字段地址，获取字段地址处的值，为某地址处的字段赋值
public class UnsafeTest01 {
    public static void main(String[] args) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        // 非静态字段
        Field intVarField    = Entry.class.getDeclaredField("intVar");
        Field doubleVarField = Entry.class.getDeclaredField("doubleVar");
        Field strVarField    = Entry.class.getDeclaredField("strVar");
        Field charArrayField = Entry.class.getDeclaredField("charArray");
        // 静态字段
        Field booConstField  = Entry.class.getDeclaredField("booConst");
        
        // 获取Unsafe实例
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        
        // 非静态字段内存地址偏移量
        long strVarOffset    = unsafe.objectFieldOffset(strVarField);
        long charArrayOffset = unsafe.objectFieldOffset(charArrayField);
        long intVarOffset    = unsafe.objectFieldOffset(intVarField);
        long doubleVarOffset = unsafe.objectFieldOffset(doubleVarField);
        // 静态字段内存地址偏移量，与普通字段获取地址的方式不同
        long booConstOffset  = unsafe.staticFieldOffset(booConstField);
        
        // 打印各字段在所属类的JVM内存中的偏移量
        System.out.println("strVarOffset -> "    + strVarOffset);
        System.out.println("charArrayOffset -> " + charArrayOffset);
        System.out.println("intVarOffset -> "    + intVarOffset);
        System.out.println("doubleVarOffset -> " + doubleVarOffset);
        System.out.println("booConstOffset -> "  + booConstOffset);
        
        
        Entry e = new Entry("沙僧", new char[]{'流', '沙', '河'}, 456, 4.56);
        
        // 获取Entry实例某JVM内存地址处对应的字段值
        System.out.println(unsafe.getObject(e, strVarOffset));
        System.out.println(Arrays.toString((char[])unsafe.getObject(e, charArrayOffset)));
        System.out.println(unsafe.getInt(e, intVarOffset));
        System.out.println(unsafe.getDouble(e, doubleVarOffset));
        System.out.println(unsafe.getBoolean(e, booConstOffset));   // 此时获取不到静态变量的正确值
        
        // 为Entry实例某JVM内存地址处对应的字段设置值
        unsafe.putObject(e, strVarOffset, "白龙马");
        unsafe.putObject(e, charArrayOffset, new char[]{'西', '海'});
        unsafe.putInt(e, intVarOffset, 567);
        unsafe.putDouble(e, doubleVarOffset, 5.67);
        unsafe.putBoolean(e, booConstOffset, false);
        
        // 查看设置完值后的输出结果
        System.out.println(unsafe.getObject(e, strVarOffset));
        System.out.println(Arrays.toString((char[])unsafe.getObject(e, charArrayOffset)));
        System.out.println(unsafe.getInt(e, intVarOffset));
        System.out.println(unsafe.getDouble(e, doubleVarOffset));
        System.out.println(unsafe.getBoolean(e, booConstOffset));   // 获取到了静态变量的正确值
    }
}
