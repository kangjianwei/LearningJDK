package test.kang.clazz.test06.模版;

// 接口中的方法都是public
public interface 接口 {
    void 接口方法_public();
    
    default void 接口方法_default() {
        // JDK 1.8 特性
    }
    
    static void 接口方法_static() {
        // JDK 1.8 特性
    }
}
