package test.kang.unsafe;

import sun.misc.Unsafe;

// 获取Unsafe实例，并借助Unsafe创建其他类的对象
public class UnsafeTest00 {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        /* 三种创建User对象的方式 */
    
        // 1. 使用反射创建对象，默认调用了无参构造方法，属性值为默认值
        Entry e1 = (Entry) Class.forName("test.kang.unsafe.Entry").newInstance();
        System.out.println(e1);
        
        
        // 2. 使用new方式创建对象，传入自定义的属性值
        Entry e2 = new Entry( "孙悟空", new char[]{'花','果','山'}, 234, 2.34);
        System.out.println(e2);
        
        
        // 3. 使用Unsafe创建对象，此创建过程不调用构造方法，所以Entry内部的基本类型默认值为0，引用类型默认值为null
        Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
        Entry e3 = (Entry) unsafe.allocateInstance(Entry.class);
        System.out.println(e3);
        
        // 4. 对e3对象赋值后，查看输出
        e3.setInfo("猪八戒", new char[]{'高','老','庄'}, 345, 3.45);
        System.out.println(e3);
    }
}
