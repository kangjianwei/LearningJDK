package test.kang.unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import sun.misc.Unsafe;

// Unsafe工具类，生成Unsafe对象的两种方式
public class UnsafeUtil {
    // 1. 通过反射获取Unsafe实例对象的字段
    public static final Unsafe getUnsafeInstance(){
        Unsafe unsafe = null;
        
        try {
            // 字段名可能会变化，比如在Android的实现中时"THE_ONE"，因此更推荐使用方式二
            Field field = Unsafe.class.getDeclaredField("theUnsafe");   // 通过反射得到Unsafe中的单例字段
            field.setAccessible(true);                                  // 设置该Field为可访问
    
            // 通过get方法得到Undafe实例对象，传入null是因为该Field为static的
            unsafe = (Unsafe) field.get(null);
        } catch(NoSuchFieldException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
    
        return unsafe;
    }
    
    // 2. 通过反射调用构造方法来生成Unsafe实例，推荐用这种方式
    public static final Unsafe getUnsafeInstance(Void v){
        Unsafe unsafe = null;
    
        try {
            Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            
            // 通过默认的私有函数构造Unsafe实例
            unsafe = unsafeConstructor.newInstance();
            
            return unsafe;
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        } catch(InstantiationException e) {
            e.printStackTrace();
        } catch(InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return unsafe;
    }
}
